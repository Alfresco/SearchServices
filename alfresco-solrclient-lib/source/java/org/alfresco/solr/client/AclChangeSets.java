package org.alfresco.solr.client;

import java.util.List;

/**
 * @author Andy
 *
 */
public class AclChangeSets
{
    private List<AclChangeSet> aclChangeSets;
    
    private Long maxChangeSetCommitTime;
    
    private Long maxChangeSetId;
    
    AclChangeSets(List<AclChangeSet> aclChangeSets, Long maxChangeSetCommitTime, Long maxChangeSetId)
    {
        this.aclChangeSets = aclChangeSets;
        this.maxChangeSetCommitTime = maxChangeSetCommitTime;
        this.maxChangeSetId = maxChangeSetId;
    }

    public List<AclChangeSet> getAclChangeSets()
    {
        return aclChangeSets;
    }

    public Long getMaxChangeSetCommitTime()
    {
        return maxChangeSetCommitTime;
    }
    
    public Long getMaxChangeSetId()
    {
        return maxChangeSetId;
    }
    
}
