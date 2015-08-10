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
package org.yamj.core.service.attachment;

import org.yamj.core.database.model.type.ImageType;

/**
 * A volatile container for content information of an attachment.
 */
public class AttachmentContent {

    private final ContentType contentType;
    private final ImageType imageType;
    private final int part;

    public AttachmentContent(ContentType contentType, ImageType imageType) {
        this(contentType, imageType, -1);
    }

    public AttachmentContent(ContentType contentType, ImageType imageType, int part) {
        this.contentType = contentType;
        this.imageType = imageType;
        this.part = part;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public ImageType getImageType() {
        return imageType;
    }

    public int getPart() {
        return part;
    }
}
