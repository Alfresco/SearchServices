package org.alfresco.solr.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Composable {@link DocRouter} is a document router that can be used standalone or nested in a primary-fallback
 * composite document routing strategy.
 * The main reason why we need this marker supertype is because the return value is different depending on how the
 * document router is used:
 *
 * <ul>
 *     <li>Standalone or leaf in a primary-fallback chain: the method will return true (node accepted) or false (node not accepted)</li>
 *     <li>
 *         Primary routing strategy in a composite primary-fallback chain:
 *         the method will return true/false if the node is accepted/refused and null
 *         if a failure is met. In this way the {@link DocRouterWithFallback} can route the request to the fallback strategy.
 *     </li>
 * </ul>
 *
 * @author agazzarini
 */
public abstract class ComposableDocRouter implements DocRouter
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final boolean isRunningInStandaloneModeOrIsLeaf;

    /**
     * Builds a doc router istance with the given mode (standalone or not).
     *
     * @param standaloneOrLeafMode a flag indicating the active mode of this router.
     */
    ComposableDocRouter(boolean standaloneOrLeafMode)
    {
        this.isRunningInStandaloneModeOrIsLeaf = standaloneOrLeafMode;
    }

    ComposableDocRouter()
    {
        this(true);
    }

    /**
     * Properly handles the return value of this doc router.
     * The return value is different depending on how the document router is used:
     *
     * <ul>
     *     <li>Standalone or leaf in a primary-fallback chain: the method will return true (node accepted) or false (node not accepted)</li>
     *     <li>
     *         Primary routing strategy in a composite primary-fallback chain:
     *         the method will return true/false if the node is accepted/refused and null
     *         if a failure is met. In this way the {@link DocRouterWithFallback} can route the request to the fallback strategy.
     *     </li>
     * </ul>
     * @return true/false or true/exception depending on the active mode of this router.
     */
    protected Boolean negativeReturnValue()
    {
        return isRunningInStandaloneModeOrIsLeaf ? false : null;
    }

    protected void debug(String message, Object ... params)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(message, params);
        }
    }
}
