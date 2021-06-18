#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
from string import Template
import yaml
import os
from distutils.dir_util import copy_tree

LIBRE_OFFICE = 'LibreOffice'
AIO_TRANSFORMERS = 'AIOTransformers'

AMQ_OPTS = '-Dmessaging.broker.url="failover:(nio://activemq:61616)?timeout=3000&jms.useCompression=true"'
TRANSFORM_OPTS = ('-DlocalTransform.core-aio.url=http://transform-core-aio:8090/ '
        '-Dalfresco-pdf-renderer.url=http://transform-core-aio:8090/ '
        '-Djodconverter.url=http://transform-core-aio:8090/ '
        '-Dimg.url=http://transform-core-aio:8090/ '
        '-Dtika.url=http://transform-core-aio:8090/ '
        '-Dtransform.misc.url=http://transform-core-aio:8090/')
SHARE_TRANSFORM_OPTS = ('-DlocalTransform.pdfrenderer.url=http://alfresco-pdf-renderer:8090/ '
        '-Dalfresco-pdf-renderer.url=http://alfresco-pdf-renderer:8090/ '
        '-DlocalTransform.imagemagick.url=http://imagemagick:8090/ -Dimg.url=http://imagemagick:8090/')
SHARDING_OPTS = '-Dsolr.useDynamicShardRegistration=true'
JAVA_OPTS = ('-Ddb.driver=org.postgresql.Driver -Ddb.username=alfresco -Ddb.password=alfresco '
        '-Ddb.url=jdbc:postgresql://postgres:5432/alfresco -Dsolr.port=8983 '
        '-Dindex.subsystem.name=solr6 '
        '-Dalfresco.restApi.basicAuthScheme=true '
        # longer timeouts for CI
        '-Dsolr.http.socket.timeout=30000 '
        '-Dsolr.http.connection.timeout=3000 ')
MTLS_OPTS = ('-Dsolr.port.ssl=8983 -Dsolr.secureComms=https ')
HTTP_OPTS = ('-Dsolr.secureComms=none')
SECRET_OPTS = ('-Dsolr.secureComms=secret -Dsolr.sharedSecret=secret')
JAVA_TOOL_OPTIONS = ('-Dencryption.keystore.type=JCEKS '
        '-Dencryption.cipherAlgorithm=DESede/CBC/PKCS5Padding '
        '-Dencryption.keyAlgorithm=DESede '
        '-Dencryption.keystore.location=/usr/local/tomcat/shared/classes/alfresco/extension/keystore/keystore '
        '-Dmetadata-keystore.password=mp6yc0UD9e '
        '-Dmetadata-keystore.aliases=metadata '
        '-Dmetadata-keystore.metadata.password=oKIWzVdEdA '
        '-Dmetadata-keystore.metadata.algorithm=DESede')
MTLS_JAVA_TOOL_OPTIONS = ('-Dencryption.keystore.type=pkcs12 -Dencryption.cipherAlgorithm=AES/CBC/PKCS5Padding '
        '-Dencryption.keyAlgorithm=DESede '
        '-Dssl-truststore.password=kT9X6oe68t -Dssl-keystore.password=kT9X6oe68t '
        '-Dssl-keystore.aliases=ssl-alfresco-ca,ssl-repo '
        '-Dssl-keystore.ssl-alfresco-ca.password=kT9X6oe68t '
        '-Dssl-keystore.ssl-repo.password=kT9X6oe68t '
        '-Dssl-truststore.aliases=alfresco-ca,ssl-repo-client '
        '-Dssl-truststore.alfresco-ca.password=kT9X6oe68t '
        '-Dssl-truststore.ssl-repo-client.password=kT9X6oe68t')

def getJavaOpts(includeAMQ, includeTransform, includeShare, solrHost, solrBaseUrl, sharding, communication):

    solrHost = '-Dsolr.host=' + solrHost
    shardingOpts = (SHARDING_OPTS if sharding != None else '')
    amqOpts = (AMQ_OPTS if includeAMQ else '')
    transformOpts = (TRANSFORM_OPTS if includeTransform else '')
    solrBaseUrlOpts = '-Dsolr.baseUrl=' + solrBaseUrl
    shareTransformOpts = (SHARE_TRANSFORM_OPTS if includeShare and includeTransform else '')
    if communication == 'mtls':
        commOpts = MTLS_OPTS
    elif communication == 'none':
        commOpts = HTTP_OPTS
    else :
        commOpts = SECRET_OPTS

    return ' '.join([JAVA_OPTS, amqOpts, transformOpts, shareTransformOpts, solrHost, solrBaseUrlOpts, shardingOpts, commOpts])

def getJavaToolOptions(communication):

    mtlsJavaToolOptions = (MTLS_JAVA_TOOL_OPTIONS if communication == 'mtls' else '')

    return ' '.join([JAVA_TOOL_OPTIONS, mtlsJavaToolOptions])

def deleteServices(dcYaml, *services):
    for service in services:
        if service in dcYaml['services'].keys():
            del(dcYaml['services'][service])

def getExtraEnvironmentVars(serviceName, replicationType):
    """Return a dict of environment variables to add to the search container declaration in docker-compose.yml."""
    extraEnvironmentVars = {}
    if replicationType == 'master':
        extraEnvironmentVars['REPLICATION_TYPE'] = 'master'
    elif replicationType == 'slave':
        extraEnvironmentVars.update({'REPLICATION_TYPE': 'slave',
                                 'REPLICATION_MASTER_HOST': serviceName.replace('slave', 'master'),
                                 'REPLICATION_MASTER_PORT': '8983',
                                 'REPLICATION_POLL_INTERVAL': '00:00:10'})
    return extraEnvironmentVars

def getSolrcoreConfig(sharding, shardId, shardCount, shardRange):
    """Returns a list of properties to add to the end of the solrcore.properties file."""
    solrcoreConfig = []
    if sharding != None:
        solrcoreConfig.append('solr.port.ssl=8983')
        solrcoreConfig.append('shard.instance={}'.format(shardId))
        solrcoreConfig.append('alfresco.port=8080')
        solrcoreConfig.append('alfresco.port.ssl=8443')
        solrcoreConfig.append('alfresco.baseUrl=/alfresco')
        if sharding not in ['DB_ID_RANGE', 'EXPLICIT_ID_FALLBACK_LRIS']:
            solrcoreConfig.append('shard.count={}'.format(shardCount))
        if sharding == 'DB_ID_RANGE':
            # The first shards each contain 800 nodes by default and the last continues the range to id 100000.
            nodesPerShard = shardRange
            rangeStart = shardId * nodesPerShard
            rangeEnd = (rangeStart + nodesPerShard - 1 if shardId < shardCount - 1 else 100000)
            solrcoreConfig.append('shard.range={}-{}'.format(rangeStart, rangeEnd))
        if sharding == 'DATE':
            solrcoreConfig.append('shard.key=cm:created')
            solrcoreConfig.append('shard.date.grouping={}'.format(12 // shardCount))
        if sharding in ['PROPERTY', 'EXPLICIT_ID', 'EXPLICIT_ID_FALLBACK_LRIS']:
            solrcoreConfig.append('shard.key=shard:shardId')
        print("SolrConfig for Shard: ", shardId, " : ", solrcoreConfig)
    return solrcoreConfig

def getSolrcoreReplacements(sharding, communication, fingerprint):
    """Returns a dict of replacements to make in the solrcore.properties file."""
    solrcoreReplacements = {}
    if fingerprint == 'true':
        solrcoreReplacements['alfresco.fingerprint=false'] = 'alfresco.fingerprint=true'
    if sharding != None:
        solrcoreReplacements['shard.method=DB_ID'] = 'shard.method={}'.format(sharding)
    if communication == 'mtls':
        solrcoreReplacements['alfresco.secureComms=none'] = 'alfresco.secureComms=https'
        solrcoreReplacements['alfresco.encryption.ssl.keystore.location=.*'] = 'alfresco.encryption.ssl.keystore.location=\\\\\\/opt\\\\\\/alfresco-search-services\\\\\\/keystore\\\\\\/ssl-repo-client.keystore'
        solrcoreReplacements['alfresco.encryption.ssl.keystore.type=.*'] = 'alfresco.encryption.ssl.keystore.type=JCEKS'
        solrcoreReplacements['alfresco.encryption.ssl.truststore.location=.*'] = 'alfresco.encryption.ssl.truststore.location=\\\\\\/opt\\\\\\/alfresco-search-services\\\\\\/keystore\\\\\\/ssl-repo-client.truststore'
        solrcoreReplacements['alfresco.encryption.ssl.truststore.type=.*'] = 'alfresco.encryption.ssl.truststore.type=JCEKS'
    elif communication == 'none':
        solrcoreReplacements['alfresco.secureComms=https'] = 'alfresco.secureComms=none'
    else :
        solrcoreReplacements['alfresco.secureComms=https'] = 'alfresco.secureComms=secret'
    return solrcoreReplacements

def addAlfrescoMtlsConfig(alfrescoArgsNode):
    """Add a list of environment values in Docker Compose Alfresco Service for mTLS."""
    alfrescoArgsNode['TRUSTSTORE_TYPE'] = 'JCEKS'
    alfrescoArgsNode['TRUSTSTORE_PASS'] = 'kT9X6oe68t'
    alfrescoArgsNode['KEYSTORE_TYPE'] = 'JCEKS'
    alfrescoArgsNode['KEYSTORE_PASS'] = 'kT9X6oe68t'
    alfrescoArgsNode['SOLR_COMMS'] = 'https'

def addAlfrescoVolumes(alfrescoNode):
    """Add route to keystores folder"""
    alfrescoNode['volumes'] = ['./keystores/alfresco:/usr/local/tomcat/alf_data/keystore']

def addSolrMtlsConfig(solrEnvNode):
    """Add a list of environment values in Docker Compose SOLR Service for mTLS."""
    solrEnvNode['SOLR_SSL_TRUST_STORE'] = '/opt/alfresco-search-services/keystore/ssl-repo-client.truststore'
    solrEnvNode['SOLR_SSL_TRUST_STORE_TYPE'] = 'JCEKS'
    solrEnvNode['SOLR_SSL_KEY_STORE'] = '/opt/alfresco-search-services/keystore/ssl-repo-client.keystore'
    solrEnvNode['SOLR_SSL_KEY_STORE_TYPE'] = 'JCEKS'
    solrEnvNode['SOLR_SSL_NEED_CLIENT_AUTH'] = 'true'

def addSolrOpts(solrEnvNode):
    """Add a list of values to add in Docker Compose SOLR_OPTS property for mTLS."""
    solrOptions = ' '.join(['-Dsolr.ssl.checkPeerName=false',
          '-Dsolr.allow.unsafe.resourceloading=true'])
    solrEnvNode['SOLR_OPTS'] = solrOptions

def addSolrJavaToolOptions(solrEnvNode):
    """Add a list of values to add in Docker Compose JAVA_TOOL_OPTIONS property for mTLS."""
    solrOptions = ' '.join(['-Dsolr.jetty.truststore.password=kT9X6oe68t ',
          '-Dsolr.jetty.keystore.password=kT9X6oe68t ',
          '-Dssl-keystore.password=kT9X6oe68t',
          '-Dssl-keystore.aliases=ssl-alfresco-ca,ssl-repo-client',
          '-Dssl-keystore.ssl-alfresco-ca.password=kT9X6oe68t',
          '-Dssl-keystore.ssl-repo-client.password=kT9X6oe68t',
          '-Dssl-truststore.password=kT9X6oe68t',
          '-Dssl-truststore.aliases=ssl-alfresco-ca,ssl-repo,ssl-repo-client',
          '-Dssl-truststore.ssl-alfresco-ca.password=kT9X6oe68t',
          '-Dssl-truststore.ssl-repo.password=kT9X6oe68t',
          '-Dssl-truststore.ssl-repo-client.password=kT9X6oe68t'])
    solrEnvNode['JAVA_TOOL_OPTIONS'] = solrOptions

def addSolrVolumes(solrNode):
    """Add route to keystores folder"""
    solrNode['volumes'] = ['./keystores/solr:/opt/alfresco-search-services/keystore']

def addSharedSecretSolrOpts(solrEnvNode):
    """Add a list of values to add in Docker Compose SOLR_OPTS property for Shared Secret communication."""
    solrEnvNode['SOLR_OPTS'] = '-Dalfresco.secureComms.secret=secret'

def makeSearchNode(outputDirectory, nodeName, externalPort, params, communication, extraEnvironmentVars={}, solrcoreConfig=[], solrcoreReplacements={}):
    # Create a dictionary for the template replacement.
    allParams = dict(params)
    allParams['SOLR_HOST'] = nodeName
    allParams['ALFRESCO_PORT'] = 8443 if communication == 'mtls' else 8080
    allParams['EXTERNAL_PORT'] = externalPort
    # Properties to add to solrcore.properties.
    allParams['SOLRCORE_PROPERTIES'] = '\\n'.join(solrcoreConfig)
    # Replacements to make in solrcore.properties (in an "abc/xyz" format suitable for sed).
    allParams['SOLRCORE_REPLACEMENTS'] = ' '.join(map(lambda pair: '"{}/{}"'.format(*pair), solrcoreReplacements.items()))

    # mTLS settings
    if communication == 'mtls':
        allParams['ALFRESCO_SECURE_COMMS'] = 'https'
    elif communication == 'none':
        allParams['ALFRESCO_SECURE_COMMS'] = 'none'
    else :
        allParams['ALFRESCO_SECURE_COMMS'] = 'secret'

    allParams['TRUSTSTORE_TYPE'] = 'JCEKS'
    allParams['KEYSTORE_TYPE'] = 'JCEKS'

    # Create a Dockerfile with any extra configuration in.
    with open(scriptDir + '/templates/search/Dockerfile.template') as f:
        dockerfileTemplate = f.read()
    dockerfileString = Template(dockerfileTemplate).substitute(allParams)
    if not os.path.isdir('{}/{}'.format(outputDirectory, nodeName)):
        os.mkdir('{}/{}'.format(outputDirectory, nodeName))
    with open('{}/{}/Dockerfile'.format(outputDirectory, nodeName), 'w') as f:
        dockerfileTemplate = f.write(dockerfileString)

    # Load the search node template.
    with open(scriptDir + '/templates/search-node.yml.template') as f:
        searchNodeTemplate = f.read()
    searchNodeString = Template(searchNodeTemplate).substitute(allParams)
    # Read the result as yaml.
    searchNodeYaml = yaml.safe_load(searchNodeString)
    # Add any extra environment variables.
    searchNodeYaml['environment'].update(extraEnvironmentVars)

    # Add mTLS configuration if required
    if communication == 'mtls':
        addSolrMtlsConfig(searchNodeYaml['environment'])
        addSolrOpts(searchNodeYaml['environment'])
        addSolrJavaToolOptions(searchNodeYaml['environment'])
        addSolrVolumes(searchNodeYaml)

    # Add shared secret configuration if required
    if communication == 'secret':
        addSharedSecretSolrOpts(searchNodeYaml['environment'])

    return searchNodeYaml

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generate a docker-compose file for ACS.')
    parser.add_argument('-a', '--alfresco', default='quay.io/alfresco/dev:acs-for-search', help='The Alfresco image')
    parser.add_argument('-s', '--search', default='quay.io/alfresco/search-services:latest', help='The Search image')
    parser.add_argument('-e', '--share', help='The Share image (or omit for no UI)')
    parser.add_argument('-p', '--postgres', default='postgres:10.1', help='The Postgres image')
    parser.add_argument('-q', '--excludeAMQ', action='store_true', help='Exclude ActiveMQ (i.e. pre-ACS 6.1)')
    parser.add_argument('-t', '--transformer', choices=[LIBRE_OFFICE, AIO_TRANSFORMERS], help='Use external transformers. '
                        + '"{}" for legacy LibreOffice (i.e. ACS 5.2.x). '.format(LIBRE_OFFICE)
                        + '"{}" for the all-in-one transformers for use with ACS 6.2.x and later.'.format(AIO_TRANSFORMERS))
    parser.add_argument('-c', '--spellcheck', action='store_true', help='Spellcheck Enabled')
    parser.add_argument('-ms', '--masterslave', action='store_true', help='Master Slave Enabled')
    parser.add_argument('-sh', '--sharding', help='Sharding method (or omit for no sharding). Note that sharding is not supported on SearchServices 1.2.x or earlier.',
                        choices=['DB_ID', 'DB_ID_RANGE', 'ACL_ID', 'MOD_ACL_ID', 'DATE', 'PROPERTY', 'LRIS', 'EXPLICIT_ID', 'EXPLICIT_ID_FALLBACK_LRIS'])
    parser.add_argument('-sc', '--shardCount', type=int, help='Total number of shards to create (default 2)')
    parser.add_argument('-sr', '--shardRange', type=int, help='Total number of nodes per shard with DB_ID_RANGE sharding (default 800)')
    parser.add_argument('-ct', '--disableCascadeTracking', action='store_true', help='Cascade Tracking Disabled')
    parser.add_argument('-ef', '--enableFingerprint', action='store_true', help='Enable Fingerprint feature')
    parser.add_argument('-ecl', '--enableCrossLocale', action='store_true', help='Enable Cross Locale configuration')
    parser.add_argument('-sl', '--searchLogLevel', default='WARN', help='The log level for search (default WARN)',
                        choices=['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'])
    parser.add_argument('-o', '--output', default='.', help='The path of the directory to output to')
    parser.add_argument('-comm', '--communication', default='none', help='Use none, mtls or secret communication between SOLR and Alfresco Repository',
                        choices=['none', 'mtls', 'secret'])
    args = parser.parse_args()

    # If sharding is selected then the default number of shards is two.
    if args.sharding != None and args.shardCount == None:
        print('Using default shardCount of 2')
        args.shardCount = 2
    elif args.sharding == None and args.shardCount != None:
        print('ERROR: shardCount={} specified without sharding method'.format(args.shardCount))
        exit(1)
    print('Arguments:', args)

    # If sharding is selected then the default number of nodes per shard is 800.
    if args.sharding == "DB_ID_RANGE" and (args.shardRange == None or args.shardRange < 1):
        print('Using default shardRange of 800')
        args.shardRange = 800
    elif args.sharding != 'DB_ID_RANGE' and args.shardRange != None:
        print('ERROR: shardRange={} is only supported for DB_ID_RANGE sharding.')
        exit(1)
    print('Arguments:', args)

    # Load the template and perform basic token substitution.
    scriptDir = os.path.dirname(os.path.realpath(__file__))
    with open(scriptDir + '/templates/docker-compose.yml.template') as f:
        template = f.read()
    params = {
            'ALFRESCO_IMAGE': args.alfresco,
            'SHARE_IMAGE': args.share,
            'POSTGRES_IMAGE': args.postgres,
            'SEARCH_IMAGE': args.search,
            'SEARCH_LOG_LEVEL': args.searchLogLevel,
            'ENABLE_CROSS_LOCALE': args.enableCrossLocale,
            'SEARCH_ENABLE_SPELLCHECK': str(args.spellcheck).lower(),
            'DISABLE_CASCADE_TRACKING': str(args.disableCascadeTracking).lower()
            }
    dcString = Template(template).substitute(params)

    # Edit the resulting yaml.
    dcYaml = yaml.safe_load(dcString)

    # Insert the search node(s).
    shardList = range(args.shardCount) if args.sharding != None else [0]
    replicationTypes = ['master', 'slave'] if args.masterslave else ['standalone']
    for shardId in shardList:
        for replicationType in replicationTypes:
            serviceName = 'search_{}_{}'.format(shardId, replicationType)
            # Workaround for ShardInfoTest.getShardInfoWithAdminAuthority.
            if shardId == 0 and replicationType == 'standalone':
                serviceName = 'search'
            externalPort = 8083 + 100 * shardId + (1 if replicationType == 'slave' else 0)
            dcYaml['services'][serviceName] = makeSearchNode(args.output, serviceName, externalPort, params, args.communication,
                      extraEnvironmentVars=getExtraEnvironmentVars(serviceName, replicationType),
                      solrcoreConfig=getSolrcoreConfig(args.sharding, shardId, args.shardCount, args.shardRange),
                      solrcoreReplacements=getSolrcoreReplacements(args.sharding, args.communication, str(args.enableFingerprint).lower()))

    # Point Alfresco at whichever Solr node came last in the list.
    solrHost = serviceName
    solrBaseUrl = '/solr-slave' if args.masterslave else '/solr'

    javaOpts = getJavaOpts(not args.excludeAMQ, args.transformer == AIO_TRANSFORMERS, args.share != None, solrHost, solrBaseUrl, args.sharding, args.communication)
    dcYaml['services']['alfresco']['environment']['JAVA_OPTS'] = javaOpts
    javaToolOpts = getJavaToolOptions(args.communication)
    dcYaml['services']['alfresco']['environment']['JAVA_TOOL_OPTIONS'] = javaToolOpts
    if args.communication == 'mtls':
        addAlfrescoMtlsConfig(dcYaml['services']['alfresco']['build']['args'])
        addAlfrescoVolumes(dcYaml['services']['alfresco'])

    if not args.share:
        deleteServices(dcYaml, 'share', 'alfresco-pdf-renderer', 'imagemagick')
    if args.excludeAMQ:
        deleteServices(dcYaml, 'activemq')
    if args.transformer != AIO_TRANSFORMERS:
        deleteServices(dcYaml, 'transform-core-aio')
        del(dcYaml['volumes']['shared-file-store-volume'])
    if args.transformer == LIBRE_OFFICE:
        dcYaml['services']['libreoffice'] = {'image': 'xcgd/libreoffice'}

    # Output the yaml.
    with open(args.output + '/docker-compose.yml', 'w') as f:
        f.write(yaml.safe_dump(dcYaml))

    # Create an Alfresco Dockerfile with any extra configuration in.
    with open(scriptDir + '/templates/alfresco/Dockerfile.template') as f:
        dockerfileTemplate = f.read()
    dockerfileString = Template(dockerfileTemplate).substitute(params)
    if not os.path.isdir('{}/{}'.format(args.output, 'alfresco')):
        os.mkdir('{}/{}'.format(args.output, 'alfresco'))
    with open('{}/{}/Dockerfile'.format(args.output, 'alfresco'), 'w') as f:
        dockerfileTemplate = f.write(dockerfileString)

    # Copy the keystores (when using mTLS)
    if args.communication == 'mtls':
        copy_tree(scriptDir + '/keystores', args.output + '/keystores')
