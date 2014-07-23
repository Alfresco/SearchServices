package org.alfresco.solr;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.opencmis.dictionary.CMISAbstractDictionaryService;
import org.alfresco.opencmis.dictionary.CMISDictionaryRegistry;
import org.alfresco.opencmis.dictionary.CMISStrictDictionaryService;
import org.alfresco.opencmis.dictionary.FilteredDictionaryComponent;
import org.alfresco.opencmis.dictionary.QNameFilter;
import org.alfresco.opencmis.mapping.CMISMapping;
import org.alfresco.opencmis.mapping.RuntimePropertyLuceneBuilderMapping;
import org.alfresco.repo.cache.MemoryCache;
import org.alfresco.repo.dictionary.DictionaryComponent;
import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.dictionary.DictionaryDAOImpl;
import org.alfresco.repo.dictionary.DictionaryNamespaceComponent;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;

/**
 * Basic Factory for creating services for the AlfrescoSolrDataMode.  It always creates a default
 *
 * @author Gethin James
 */
public class AlfrescoClientDataModelServicesFactory
{
    public static final String DICTIONARY_FILTERED_WITH_EXCLUSIONS = "cmisWithExclusions";

    /**
     * Constructs a dictionary by default.
     * 
     * @param cmisMapping
     * @param dictionaryService
     * @param dictionaryDAO
     * @return Map<String,CMISDictionaryService> 
     */
    public static Map<DictionaryKey,CMISAbstractDictionaryService> constructDictionaries(QNameFilter qnameFilter, NamespaceDAO namespaceDAO,
    		DictionaryComponent dictionaryService, DictionaryDAO dictionaryDAO) 
    {
        DictionaryNamespaceComponent namespaceService = new DictionaryNamespaceComponent();
        namespaceService.setNamespaceDAO(namespaceDAO);

        CMISMapping cmisMapping = new CMISMapping();
        cmisMapping.setCmisVersion(CmisVersion.CMIS_1_0);
        cmisMapping.setFilter(qnameFilter);
        cmisMapping.setNamespaceService(namespaceService);
        cmisMapping.setDictionaryService(dictionaryService);
        cmisMapping.afterPropertiesSet();

        CMISMapping cmisMapping11 = new CMISMapping();
        cmisMapping11.setCmisVersion(CmisVersion.CMIS_1_1);
        cmisMapping11.setFilter(qnameFilter);
        cmisMapping11.setNamespaceService(namespaceService);
        cmisMapping11.setDictionaryService(dictionaryService);
        cmisMapping11.afterPropertiesSet();

        Map<DictionaryKey,CMISAbstractDictionaryService> dictionaries = new HashMap<DictionaryKey,CMISAbstractDictionaryService>();

        DictionaryKey key = new DictionaryKey(CmisVersion.CMIS_1_0, CMISStrictDictionaryService.DEFAULT);
        dictionaries.put(key, newInstance(cmisMapping, dictionaryService, dictionaryDAO));
        CMISMapping mappingWithExclusions = newInstanceOfExcludedCMISMapping(cmisMapping, qnameFilter);
        key = new DictionaryKey(CmisVersion.CMIS_1_0, DICTIONARY_FILTERED_WITH_EXCLUSIONS);
        dictionaries.put(key, newInstance(mappingWithExclusions, dictionaryService, dictionaryDAO));
        
        key = new DictionaryKey(CmisVersion.CMIS_1_1, CMISStrictDictionaryService.DEFAULT);
        dictionaries.put(key, newInstance(cmisMapping11, dictionaryService, dictionaryDAO));
        CMISMapping mappingWithExclusions11 = newInstanceOfExcludedCMISMapping(cmisMapping11, qnameFilter);
        key = new DictionaryKey(CmisVersion.CMIS_1_1, DICTIONARY_FILTERED_WITH_EXCLUSIONS);
        dictionaries.put(key, newInstance(mappingWithExclusions11, dictionaryService, dictionaryDAO));

        return dictionaries;
    }

    /**
     * Constructs a dictionary by default.
     * 
     * @param dictionaryDAO
     * @return Map<String,CMISDictionaryService> 
     */
    public static Map<String, DictionaryComponent> constructDictionaryServices(QNameFilter qnameFilter, DictionaryDAOImpl dictionaryDAO)
    {
        Map<String,DictionaryComponent> dictionaries = new HashMap<String,DictionaryComponent>();
        DictionaryComponent compo = new DictionaryComponent();
        compo.setDictionaryDAO(dictionaryDAO);
        dictionaries.put(CMISStrictDictionaryService.DEFAULT, compo);
        FilteredDictionaryComponent fdc  = new FilteredDictionaryComponent();
        fdc.setDictionaryDAO(dictionaryDAO);
        fdc.setFilter(qnameFilter);
        dictionaries.put(DICTIONARY_FILTERED_WITH_EXCLUSIONS, fdc);
        return dictionaries;
        
    }
    
    private static CMISMapping newInstanceOfExcludedCMISMapping(CMISMapping cmisMapping, QNameFilter filter)
    {
        CMISMapping cmisMappingWithExcl = new CMISMapping();
        cmisMappingWithExcl.setNamespaceService(cmisMapping.getNamespaceService());
        cmisMappingWithExcl.setDictionaryService(cmisMapping.getDictionaryService());
        cmisMappingWithExcl.setFilter(filter);
        cmisMappingWithExcl.setCmisVersion(cmisMapping.getCmisVersion());
        cmisMappingWithExcl.afterPropertiesSet();
        return cmisMappingWithExcl;
    }
    
    protected static CMISStrictDictionaryService newInstance(CMISMapping cmisMapping, DictionaryService dictionaryService, DictionaryDAO dictionaryDAO)
    {
        CMISStrictDictionaryService cmisDictionaryService = new CMISStrictDictionaryService();
        cmisDictionaryService.setCmisMapping(cmisMapping);
        cmisDictionaryService.setDictionaryService(dictionaryService);
        cmisDictionaryService.setDictionaryDAO(dictionaryDAO);
        cmisDictionaryService.setSingletonCache(new MemoryCache<String, CMISDictionaryRegistry>());

        RuntimePropertyLuceneBuilderMapping luceneBuilderMapping = new RuntimePropertyLuceneBuilderMapping();
        luceneBuilderMapping.setDictionaryService(dictionaryService);
        luceneBuilderMapping.setCmisDictionaryService(cmisDictionaryService);
        cmisDictionaryService.setPropertyLuceneBuilderMapping(luceneBuilderMapping);
        luceneBuilderMapping.afterPropertiesSet();
        cmisDictionaryService.init();
        return cmisDictionaryService;
    }

    public static class DictionaryKey
    {
    	private CmisVersion cmisVersion;
    	private String key;
		public DictionaryKey(CmisVersion cmisVersion, String key) {
			super();
			this.cmisVersion = cmisVersion;
			this.key = key;
		}
		public CmisVersion getCmisVersion() {
			return cmisVersion;
		}
		public String getKey() {
			return key;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((cmisVersion == null) ? 0 : cmisVersion.hashCode());
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DictionaryKey other = (DictionaryKey) obj;
			if (cmisVersion != other.cmisVersion)
				return false;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}
    }
}
