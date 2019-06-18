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
        type: 'input',
        name: 'acsVersion',
        message: 'Which Alfresco version do you want to use?',
        default: '6.1'
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
        when: function (response) {
          return response.httpMode == 'http' || commandProps['httpMode'] == 'http';
        },
        type: 'confirm',
        name: 'replication',
        message: 'Would you like to use SOLR Replication (2 nodes in master-slave)?',
        default: false
      },
      // Enterprise only options
      {
        when: function (response) {
          return (response.alfrescoVersion == 'enterprise' || commandProps['alfrescoVersion'] == 'enterprise') &&
                 (!response.replication && !commandProps['replication']);
        },
        type: 'confirm',
        name: 'sharding',
        message: 'Would you like to use dynamic Sharding (2 SOLR nodes)?',
        default: false
      },
      {
        when: function (response) {
          return (response.alfrescoVersion == 'enterprise' || commandProps['alfrescoVersion'] == 'enterprise') &&
                 (response.sharding || commandProps['sharding']);
        },
        type: 'confirm',
        name: 'explicitRouting',
        message: 'Would you like to use SOLR Explicit Routing instead of DB_ID for the Shards?',
        default: false
      },
      {
        when: function (response) {
          return response.alfrescoVersion == 'enterprise' || commandProps['alfrescoVersion'] == 'enterprise';
        },
        type: 'confirm',
        name: 'insightEngine',
        message: 'Would you like to use Insight Engine instead of Search Services?',
        default: false
      },
      {
        when: function (response) {
          return (response.alfrescoVersion == 'enterprise' || commandProps['alfrescoVersion'] == 'enterprise') &&
                 (response.insightEngine || commandProps['insightEngine']);
        },
        type: 'confirm',
        name: 'zeppelin',
        message: 'Would you like to deploy Zeppelin?',
        default: false
      }
    ];

    // Read options from command line parameters
    const filteredPrompts = [];
    const commandProps = new Map();
    prompts.forEach(function prompts(prompt) {
      const option = this.options[prompt.name];
      if (option === undefined) {
        filteredPrompts.push(prompt);
      } else {      
        commandProps[prompt.name] = normalize(option, prompt); 
      }
    }, this);

    // Prompt only for parameters not passed by command line
    return this.prompt(filteredPrompts).then(props => {
      this.props = props;
      Object.assign(props, commandProps);
    });

  }

  // Generate boilerplate from "templates" folder
  writing() {

    // Docker Compose environment variables values
    this.fs.copy(
      this.templatePath(this.props.acsVersion + '/.env'),
      this.destinationPath('.env'),
    )

    // Base Docker Compose Template
    const dockerComposeTemplate = 
        (this.props.alfrescoVersion == 'community' ? 
          this.props.acsVersion + '/docker-compose-ce.yml' :
          this.props.acsVersion + '/docker-compose-ee.yml');

    // Repository Docker Image tag
    const acsImageTag = 
      (this.props.alfrescoVersion == 'community' ? 
        'alfresco/alfresco-content-repository-community' :
        'alfresco/alfresco-content-repository');

    // Repository Docker Image version (from environmen variable)
    const acsEnvTag =
      (this.props.alfrescoVersion == 'community' ? 
        'ALFRESCO_CE_TAG' :
        'ALFRESCO_TAG');

    // Search Docker Image tag
    const searchImageTag = 
    (this.props.insightEngine ? 
      'quay.io/alfresco/insight-engine' :
      'quay.io/alfresco/search-services');
    
    // Search Docker Image version (from environmen variable)
    const searchEnvTag =
      (this.props.insightEngine ? 
        'SEARCH_TAG' :
        'SEARCH_CE_TAG');

    // Search Docker Image installation base path
    const searchBasePath = 
      (this.props.insightEngine ?
        "alfresco-insight-engine" :
        "alfresco-search-services");

    // Copy Docker Compose applying configuration
    this.fs.copyTpl(
      this.templatePath(dockerComposeTemplate),
      this.destinationPath('docker-compose.yml'),
      { httpMode: this.props.httpMode,
        secureComms: (this.props.httpMode == 'http' ? 'none' : 'https'),
        acsTag: acsEnvTag,
        alfrescoPort: (this.props.httpMode == 'http' ? '8080' : '8443'),
        replication: (this.props.replication ? "true" : "false"),
        searchSolrHost: (this.props.replication ? "solr6secondary" : "solr6"),
        searchTag: searchEnvTag,
        searchPath: searchBasePath,
        zeppelin: (this.props.zeppelin ? "true" : "false"),
        sharding: (this.props.sharding ? "true" : "false"),
        explicitRouting: (this.props.explicitRouting ? "true" : "false")
      }
    );

    // Copy Docker Image for Repository applying configuration
    this.fs.copyTpl(
      this.templatePath(this.props.acsVersion + '/alfresco/Dockerfile'),
      this.destinationPath('alfresco/Dockerfile'),
      { 
        acsImage: acsImageTag,
        sharding: (this.props.sharding ? "true" : "false")
      }
    );
    if (this.props.sharding) {
      this.fs.copy(
        this.templatePath(this.props.acsVersion + '/alfresco/model'),
        this.destinationPath('alfresco/model')
      )
    }

    // Copy Docker Image for Search applying configuration
    this.fs.copyTpl(
      this.templatePath(this.props.acsVersion + '/search'),
      this.destinationPath('search'),
      {
        searchImage: searchImageTag,
        searchTag: searchEnvTag,
        searchPath: searchBasePath
      }
    );

    // Copy Docker Image for Zeppelin applying configuration
    if (this.props.zeppelin) {
      this.fs.copy(
        this.templatePath(this.props.acsVersion + '/zeppelin'),
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

