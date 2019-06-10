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
 * - Clustering (master-slave)
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
        name: 'clustering',
        message: 'Would you like to use a SOLR Cluster (2 nodes in master-slave)?',
        default: false
      },
      // Enterprise only options
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
      },
      {
        when: function (response) {
          return (response.alfrescoVersion == 'enterprise' || commandProps['alfrescoVersion'] == 'enterprise') &&
                 (!response.clustering || !commandProps['clustering']);
        },
        type: 'confirm',
        name: 'sharding',
        message: 'Would you like to use dynamic Sharding (2 SOLR nodes)?',
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

    // Base Docker Image for Community
    if (this.props.alfrescoVersion == 'community') {
      this.fs.copyTpl(
        this.templatePath(this.props.acsVersion + '/docker-compose-ce.yml'),
        this.destinationPath('docker-compose.yml'),
        { httpMode: this.props.httpMode,
          secureComms: (this.props.httpMode == 'http' ? 'none' : 'https'),
          alfrescoPort: (this.props.httpMode == 'http' ? '8080' : '8443'),
          clustering: (this.props.clustering ? "true" : "false"),
          searchSolrHost: (this.props.clustering ? "solr6slave" : "solr6")
        }
      );
    
    // Base Docker Image for Enterprise
    } else {
      this.fs.copyTpl(
        this.templatePath(this.props.acsVersion + '/docker-compose-ee.yml'),
        this.destinationPath('docker-compose.yml'),
        { httpMode: this.props.httpMode,
          secureComms: (this.props.httpMode == 'http' ? 'none' : 'https'),
          alfrescoProtocol: (this.props.httpMode == 'http' ? 'http' : 'https'),
          alfrescoPort: (this.props.httpMode == 'http' ? '8080' : '8443'),
          searchImage: (this.props.insightEngine ?
            "quay.io/alfresco/insight-engine" :
            "quay.io/alfresco/search-services"
          ),
          searchTag: (this.props.insightEngine ?
            "SEARCH_TAG" :
            "SEARCH_CE_TAG"
          ),
          searchPath: (this.props.insightEngine ?
            "alfresco-insight-engine" :
            "alfresco-search-services"
          ),
          zeppelin: (this.props.zeppelin ? "true" : "false"),
          sharding: (this.props.sharding ? "true" : "false"),
          clustering: (this.props.clustering ? "true" : "false"),
          searchSolrHost: (this.props.clustering ? "solr6slave" : "solr6")
        }
      );
    }

    // Docker Compose environment variables values
    this.fs.copy(
      this.templatePath(this.props.acsVersion + '/.env'),
      this.destinationPath('.env'),
    )

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
      this.fs.copyTpl(
        this.templatePath(this.props.acsVersion + '/alfresco-https'),
        this.destinationPath('alfresco-https'),
        { 
          acsImage: (this.props.alfrescoVersion == 'community' ?
          "alfresco/alfresco-content-repository-community" :
          "alfresco/alfresco-content-repository")
        }
      )
      if (!this.props.sharding) {
        this.fs.copyTpl(
          this.templatePath(this.props.acsVersion + '/search-https'),
          this.destinationPath('search-https'),
          {
            searchImage: (this.props.insightEngine ?
            "quay.io/alfresco/insight-engine" :
            "alfresco/alfresco-search-services"
            ),
            searchPath: (this.props.insightEngine ?
              "alfresco-insight-engine" :
              "alfresco-search-services"
            )
          }
        )
      }
      if (this.props.zeppelin == true) {
        this.fs.copy(
          this.templatePath(this.props.acsVersion + '/zeppelin-https'),
          this.destinationPath('zeppelin-https')
        )
        this.fs.copy(
          this.templatePath('keystores/zeppelin'),
          this.destinationPath('keystores/zeppelin')
        )
      }
    }

    // Copy replication configuration
    if (this.props.clustering) {
      this.fs.copyTpl(
        this.templatePath(this.props.acsVersion + '/replication-none'),
        this.destinationPath('replication-none'),
        {
          searchImage: (this.props.insightEngine ?
          "quay.io/alfresco/insight-engine" :
          "alfresco/alfresco-search-services"
          ),
          searchPath: (this.props.insightEngine ?
            "alfresco-insight-engine" :
            "alfresco-search-services"
          )
        }
      )
    }

    // Copy sharding configuration
    if (this.props.sharding) {
      if (this.props.httpMode == 'https') {
        this.fs.copyTpl(
          this.templatePath(this.props.acsVersion + '/sharding-https'),
          this.destinationPath('sharding-https'),
          {
            searchImage: (this.props.insightEngine ?
              "quay.io/alfresco/insight-engine" :
              "quay.io/alfresco/search-services"
            )
          }
        )
      } else {
        this.fs.copyTpl(
          this.templatePath(this.props.acsVersion + '/sharding-none'),
          this.destinationPath('sharding-none'),
          {
            searchImage: (this.props.insightEngine ?
              "quay.io/alfresco/insight-engine" :
              "quay.io/alfresco/search-services"
            )
          }
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

