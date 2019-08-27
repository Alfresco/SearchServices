'use strict';
const Generator = require('yeoman-generator');
var banner = require('./banner')

/**
 * This module buids a Docker Compose template to test
 * Repository and Search Services/Insight Engine in
 * different configurations:
 * - Plain HTTP communications
 * - TLS/SSL Mutual Authentication communications
 * - Sharding (dynamic)
 * - Replication (master-slave)
*/
module.exports = class extends Generator {

  // Options to be chosen by the user
  prompting() {

    if (!this.options['skip-install-message']) {
      this.log(banner);
    }

    const prompts = [
      {
        type: 'list',
        name: 'acsVersion',
        message: 'Which ACS version do you want to use?',
        choices: [ "6.1", "6.2" ],
        default: '6.2'
      },
      {
        whenFunction: response => response.acsVersion == '6.1',
        type: 'confirm',
        name: 'ags',
        message: 'Would you like to use AGS?',
        default: false
      },
      {
        type: 'list',
        name: 'alfrescoVersion',
        message: 'Would you like to use Alfresco enterprise or community?',
        choices: [ "community", "enterprise" ],
        default: 'community'
      },
      {
        type: 'list',
        name: 'httpMode',
        message: 'Would you like to use http or https?',
        choices: [ "http", "https" ],
        default: 'http'
      },
      {
        whenFunction: response => response.httpMode == 'http',
        type: 'confirm',
        name: 'replication',
        message: 'Would you like to use SOLR Replication (2 nodes in master-slave)?',
        default: false
      },
      // Enterprise only options
      {
        whenFunction: response => response.alfrescoVersion == 'enterprise' && !response.replication,
        type: 'confirm',
        name: 'sharding',
        message: 'Would you like to use dynamic Sharding (2 SOLR nodes)?',
        default: false
      },
      {
        whenFunction: response => response.alfrescoVersion == 'enterprise' && response.sharding,
        type: 'list',
        name: 'shardingMethod',
        message: 'Which Sharding Method do you want to use?',
        choices: [ 
          "DB_ID", 
          "DB_ID_RANGE", 
          "ACL_ID", 
          "MOD_ACL_ID", 
          "DATE", 
          "PROPERTY", 
          "LAST_REGISTERED_INDEXING_SHARD",
          "EXPLICIT_ID_FALLBACK_LRIS",
          "EXPLICIT_ID"
         ],
        default: 'DB_ID'
      },
      {
        whenFunction: response => response.alfrescoVersion == 'enterprise',
        type: 'confirm',
        name: 'insightEngine',
        message: 'Would you like to use Insight Engine instead of Search Services?',
        default: false
      },
      {
        whenFunction: response => response.alfrescoVersion == 'enterprise' && response.insightEngine,
        type: 'confirm',
        name: 'zeppelin',
        message: 'Would you like to deploy Zeppelin?',
        default: false
      }
    ];

    // Create a chain of promises containing the prompts.
    this.promise = Promise.resolve();
    this.props = {};
    prompts.forEach(prompt => {
      // Check if we can answer the prompt via a command line argument.
      const option = this.options[prompt.name];
      if (option === undefined) {
        this.promise = this.promise.then(_ => {
          // Check if the prompt is valid given the existing settings.
          if (!prompt.whenFunction || prompt.whenFunction(this.props)) {
            // Display the prompt and update this.props with the response.
            return this.prompt(prompt).then(props => Object.assign(this.props, props));
          }
        });
      } else {
        this.props[prompt.name] = normalize(option, prompt);
      }
    });
    // Provide Yeoman with the chain of promises so it will wait for answers.
    return this.promise;
  }

  // Generate boilerplate from "templates" folder
  writing() {

    var dockerComposeTemplateDirectory = '6.2';
    if (this.props.acsVersion.startsWith('6.1')) {
      dockerComposeTemplateDirectory = '6.1';
    }

    // Docker Compose environment variables values
    this.fs.copyTpl(
      this.templatePath(dockerComposeTemplateDirectory + '/.env'),
      this.destinationPath('.env'),
      {
        acs: (this.props.ags ? "false" : "true"),
        ags: (this.props.ags ? "true" : "false")
      }
    )

    // Base Docker Compose Template
    const dockerComposeTemplate =
        (this.props.alfrescoVersion == 'community' ?
          dockerComposeTemplateDirectory + '/docker-compose-ce.yml' :
          dockerComposeTemplateDirectory + '/docker-compose-ee.yml');

    // Repository Docker Image name
    const acsImageName =
      (this.props.alfrescoVersion == 'community' ?
        (this.props.ags ? 
          'alfresco/alfresco-governance-repository-community' :
          'alfresco/alfresco-content-repository-community') :
        (this.props.ags ? 
          'quay.io/alfresco/alfresco-governance-repository-enterprise':
          'alfresco/alfresco-content-repository'
        )
      );

    // Share Docker Image name
    const shareImageName =
      (this.props.alfrescoVersion == 'community' ?
        (this.props.ags ? 
          'alfresco/alfresco-governance-share-community' :
          'alfresco/alfresco-share') :
        (this.props.ags ? 
          'quay.io/alfresco/alfresco-governance-share-enterprise':
          'alfresco/alfresco-share'
        )
      );

    // Search Docker Image
    const searchImageName =
    (this.props.insightEngine ?
      'quay.io/alfresco/insight-engine' :
      'quay.io/alfresco/search-services');

    // Search Docker Image installation base path
    const searchBasePath =
      (this.props.insightEngine ?
        "alfresco-insight-engine" :
        "alfresco-search-services");

    // Copy Docker Compose applying configuration
    this.fs.copyTpl(
      this.templatePath(dockerComposeTemplate),
      this.destinationPath('docker-compose.yml'),
      {
        httpMode: this.props.httpMode,
        secureComms: (this.props.httpMode == 'http' ? 'none' : 'https'),
        alfrescoPort: (this.props.httpMode == 'http' ? '8080' : '8443'),
        replication: (this.props.replication ? "true" : "false"),
        searchSolrHost: (this.props.replication ? "solr6secondary" : "solr6"),
        searchPath: searchBasePath,
        zeppelin: (this.props.zeppelin ? "true" : "false"),
        sharding: (this.props.sharding ? "true" : "false"),
        shardingMethod: (this.props.shardingMethod)
      }
    );

    var imagesDirectory = 'images';

    // Copy Docker Image for Repository applying configuration
    this.fs.copyTpl(
      this.templatePath(imagesDirectory + '/alfresco/Dockerfile'),
      this.destinationPath('alfresco/Dockerfile'),
      {
        acsImage: acsImageName,
        sharding: (this.props.sharding ? "true" : "false")
      }
    );
    // Copy Sharding Content Model or an empty file to allow model deployments
    if (this.props.sharding) {
      this.fs.copy(
        this.templatePath(imagesDirectory + '/alfresco/model/*.xml'),
        this.destinationPath('alfresco/model/')
      )
    } else {
      this.fs.copy(
        this.templatePath(imagesDirectory + '/alfresco/model/empty'),
        this.destinationPath('alfresco/model/empty')
      )
    }

    // Copy Docker Image for Share applying configuration
    this.fs.copyTpl(
      this.templatePath(imagesDirectory + '/share/Dockerfile'),
      this.destinationPath('share/Dockerfile'),
      {
        shareImage: shareImageName
      }
    );
    // Copy Sharding Content Forms or an empty file to allow forms deployments
    if (this.props.sharding) {
      this.fs.copy(
        this.templatePath(imagesDirectory + '/share/model/sharding-share-config-custom.xml'),
        this.destinationPath('share/model/share-config-custom-dev.xml')
      )
    } else {
      this.fs.copy(
        this.templatePath(imagesDirectory + '/share/model/empty.xml'),
        this.destinationPath('share/model/share-config-custom-dev.xml')
      )
    }

    // Copy Docker Image for Search applying configuration
    this.fs.copyTpl(
      this.templatePath(imagesDirectory + '/search'),
      this.destinationPath('search'),
      {
        searchImage: searchImageName,
        searchPath: searchBasePath
      }
    );

    // Copy Docker Image for Zeppelin applying configuration
    if (this.props.zeppelin) {
      this.fs.copy(
        this.templatePath(imagesDirectory + '/zeppelin'),
        this.destinationPath('zeppelin')
      );
    }

    // Add resources for SSL configuration
    if (this.props.httpMode == 'https') {
      this.fs.copy(
        this.templatePath('keystores/alfresco'),
        this.destinationPath('keystores/alfresco')
      )
      this.fs.copy(
        this.templatePath('keystores/solr'),
        this.destinationPath('keystores/solr')
      )
      this.fs.copy(
        this.templatePath('keystores/client'),
        this.destinationPath('keystores/client')
      )
      if (this.props.zeppelin == true) {
        this.fs.copy(
          this.templatePath('keystores/zeppelin'),
          this.destinationPath('keystores/zeppelin')
        )
      }
    }

  }

};

// Convert parameter string value to boolean value
function normalize(option, prompt) {

  if (prompt.type === 'confirm' && typeof option === 'string') {
    let lc = option.toLowerCase();
    if (lc === 'true' || lc === 'false') {
      return (lc === 'true');
    } else {
      return option;
    }
  }

  return option;

}

