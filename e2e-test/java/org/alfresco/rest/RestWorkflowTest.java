package org.alfresco.rest;

import org.alfresco.utility.data.DataWorkflow;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class RestWorkflowTest extends RestTest
{
    @Autowired
    protected DataWorkflow dataWorkflow;    
}
