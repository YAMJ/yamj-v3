/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.service.mediainfo;

import java.io.InputStream;
import org.apache.commons.io.IOUtils;

public class MediaInfoStream implements AutoCloseable {

    private final Process process;
    private final InputStream inputStream;

    public MediaInfoStream(Process process) {
        this.process = process;
        this.inputStream = process.getInputStream();
    }

    public MediaInfoStream(String content) {
        this.process = null;
        this.inputStream = IOUtils.toInputStream(content);
    }
    
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void close() throws Exception {
        if (process != null) {
            try {
                process.waitFor();
            } catch (Exception ignore)  {/*ignore*/}
        }
            
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ignore)  {/*ignore*/}
        }
        
        if (process != null) {
            try {
                if (process.getErrorStream() != null) {
                    process.getErrorStream().close();  
                }
            } catch (Exception ignore)  {/*ignore*/}
            try {
                if (process.getOutputStream() != null) {
                    process.getOutputStream().close();
                }
            } catch (Exception ignore) {/*ignore*/}

            try {
                process.destroy();
            } catch (Exception ignore)  {/*ignore*/}
        }
    }
}
