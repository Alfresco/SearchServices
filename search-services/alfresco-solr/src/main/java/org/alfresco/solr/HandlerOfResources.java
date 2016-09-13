package org.alfresco.solr;

import org.apache.solr.common.params.SolrParams;

import java.io.*;

/**
 * Created by gethin on 13/09/16.
 */
public class HandlerOfResources {


    /**
     * Note files can alter due to background processes so file not found is Ok
     *
     * @param srcDir
     * @param destDir
     * @param preserveFileDate
     * @throws IOException
     */
    public static void copyDirectory(File srcDir, File destDir, boolean preserveFileDate) throws IOException
    {
        if (destDir.exists())
        {
            throw new IOException("Destination should be created from clean");
        }
        else
        {
            if (!destDir.mkdirs()) { throw new IOException("Destination '" + destDir + "' directory cannot be created"); }
            if (preserveFileDate)
            {
                // OL if file not found so does not need to check
                destDir.setLastModified(srcDir.lastModified());
            }
        }
        if (!destDir.canWrite()) { throw new IOException("No access to destination directory" + destDir); }

        File[] files = srcDir.listFiles();
        if (files != null)
        {
            for (int i = 0; i < files.length; i++)
            {
                File currentCopyTarget = new File(destDir, files[i].getName());
                if (files[i].isDirectory())
                {
                    copyDirectory(files[i], currentCopyTarget, preserveFileDate);
                }
                else
                {
                    copyFile(files[i], currentCopyTarget, preserveFileDate);
                }
            }
        }
    }

    public static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException
    {
        try
        {
            if (destFile.exists()) { throw new IOException("File shoud not exist " + destFile); }

            FileInputStream input = new FileInputStream(srcFile);
            try
            {
                FileOutputStream output = new FileOutputStream(destFile);
                try
                {
                    copy(input, output);
                }
                finally
                {
                    try
                    {
                        output.close();
                    }
                    catch (IOException io)
                    {

                    }
                }
            }
            finally
            {
                try
                {
                    input.close();
                }
                catch (IOException io)
                {

                }
            }

            // check copy
            if (srcFile.length() != destFile.length()) { throw new IOException("Failed to copy full from '" + srcFile
                    + "' to '" + destFile + "'"); }
            if (preserveFileDate)
            {
                destFile.setLastModified(srcFile.lastModified());
            }
        }
        catch (FileNotFoundException fnfe)
        {
            fnfe.printStackTrace();
        }
    }

    public static int copy(InputStream input, OutputStream output) throws IOException
    {
        byte[] buffer = new byte[2048 * 4];
        int count = 0;
        int n = 0;
        while ((n = input.read(buffer)) != -1)
        {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public void deleteDirectory(File directory) throws IOException
    {
        if (!directory.exists()) { return; }
        if (!directory.isDirectory()) { throw new IllegalArgumentException("Not a directory " + directory); }

        File[] files = directory.listFiles();
        if (files == null) { throw new IOException("Failed to delete director - no access" + directory); }

        for (int i = 0; i < files.length; i++)
        {
            File file = files[i];

            if (file.isDirectory())
            {
                deleteDirectory(file);
            }
            else
            {
                if (!file.delete()) { throw new IOException("Unable to delete file: " + file); }
            }
        }

        if (!directory.delete()) { throw new IOException("Unable to delete directory " + directory); }
    }


    public static boolean getSafeBoolean(SolrParams params, String paramName)
    {
        boolean paramValue = false;
        if (params.get(paramName) != null)
        {
            paramValue = Boolean.valueOf(params.get(paramName));
        }
        return paramValue;
    }

    public static Long getSafeLong(SolrParams params, String paramName)
    {
        Long paramValue = null;
        if (params.get(paramName) != null)
        {
            paramValue = Long.valueOf(params.get(paramName));
        }
        return paramValue;
    }
}
