/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.athento.nuxeo.converter.plugins;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.convert.plugins.CommandLineBasedConverter;
import org.nuxeo.runtime.api.Framework;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pdf2HtmlEx converter based on the pdf2htmlEx.
 *
 * @author victorsanchez
 */
public class PDF2HtmlExConverter extends CommandLineBasedConverter {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(PDF2HtmlExConverter.class);

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput,
                                     CmdParameters cmdParams) {
        CmdParameters.ParameterValue outputPath = cmdParams.getParameters().get("outDirPath");
        File outputDir = new File(outputPath.getValue());
        File[] files = outputDir.listFiles();
        List<Blob> blobs = new ArrayList<Blob>();
        if (files.length > 0) {
            blobs.add(new FileBlob(files[0]));
        }
        return new SimpleCachableBlobHolder(blobs);
    }

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder,
                                                     Map<String, Serializable> parameters) throws ConversionException {
        Map<String, Blob> cmdBlobParams = new HashMap<String, Blob>();
        try {
            cmdBlobParams.put("inFilePath", blobHolder.getBlob());
        } catch (ClientException e) {
            throw new ConversionException("Unable to get Blob for holder", e);
        }
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder,
                                                         Map<String, Serializable> parameters) throws ConversionException {

        Map<String, String> cmdStringParams = new HashMap<String, String>();

        String baseDir = getTmpDirectory(parameters);
        Path tmpPath = new Path(baseDir).append("pdf2htmlex_"
                + System.currentTimeMillis());

        File outDir = new File(tmpPath.toString());
        boolean dirCreated = outDir.mkdir();
        if (!dirCreated) {
            throw new ConversionException(
                    "Unable to create tmp dir for transformer output");
        }
        cmdStringParams.put("outDirPath", outDir.getAbsolutePath());
        cmdStringParams.put("zoom", Framework.getProperty("pdf2htmlex.zoom", "1.3"));
        if (LOG.isInfoEnabled()) {
            LOG.info("outDir=" + outDir.getAbsolutePath() + " " + cmdStringParams);
        }
        return cmdStringParams;
    }

}
