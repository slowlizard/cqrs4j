/*
 * Copyright (c) 2009. Gridshore
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.gridshore.cqrs.repository.eventsourcing;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import nl.gridshore.cqrs.DomainEvent;
import nl.gridshore.cqrs.EventStream;
import nl.gridshore.cqrs.repository.ObjectInputStreamAdapter;
import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;

/**
 * @author Allard Buijze
 */
public class XStreamEventStore implements EventStore {

    private XStream xStream;
    private Resource baseDir;

    public XStreamEventStore() {
        xStream = new XStream();
        xStream.registerConverter(new SingleValueConverter() {
            @Override
            public boolean canConvert(Class type) {
                return type.equals(LocalDateTime.class);
            }

            @Override
            public String toString(Object obj) {
                return obj.toString();
            }

            @Override
            public Object fromString(String str) {
                return new LocalDateTime(str);
            }
        });

    }

    @Override
    public void appendEvents(String type, EventStream eventsToStore) {
        OutputStream out = null;
        try {
            File eventFile = getBaseDirForType(type).createRelative(eventsToStore.getAggregateIdentifier() + ".events")
                    .getFile();
            out = new FileOutputStream(eventFile, true);
            CompactWriter writer = new CompactWriter(new OutputStreamWriter(out, "UTF-8"));
            while (eventsToStore.hasNext()) {
                DomainEvent event = eventsToStore.next();
                xStream.marshal(event, writer);
                IOUtils.write("\n", out);
            }
        } catch (IOException e) {
            throw new EventStorageException("Unable to store given entity due to a IOException", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    @Override
    public EventStream readEvents(String type, UUID identifier) {
        try {
            File eventFile = getBaseDirForType(type).createRelative(identifier + ".events").getFile();
            FileInputStream fileStream = new FileInputStream(eventFile);
            InputStream inputStream = surroundWitObjectStreamTag(fileStream);
            ObjectInputStream eventsStream = xStream.createObjectInputStream(inputStream);
            return new ObjectInputStreamAdapter(eventsStream);
        } catch (IOException e) {
            throw new IllegalStateException("No such file", e);
        }
    }

    private InputStream surroundWitObjectStreamTag(FileInputStream fileStream) throws UnsupportedEncodingException {
        InputStream prefix = new ByteArrayInputStream("<object-stream>".getBytes("UTF-8"));
        InputStream suffix = new ByteArrayInputStream("</object-stream>".getBytes("UTF-8"));
        return new SequenceInputStream(prefix, new SequenceInputStream(fileStream, suffix));
    }

    protected Resource getBaseDirForType(String type) {
        try {
            Resource typeSpecificDir = baseDir.createRelative("/" + type + "/");
            if (!typeSpecificDir.exists() && !typeSpecificDir.getFile().mkdirs()) {
                throw new IllegalStateException("The given event store directory doesn't exist and could not be created");
            }
            return typeSpecificDir;
        } catch (IOException e) {
            // TODO: Real error handling here
            e.printStackTrace();
            return baseDir;
        }
    }

    @Required
    public void setBaseDir(Resource baseDir) {
        this.baseDir = baseDir;
    }

    public void setAliases(Map<String, Class> aliases) {
        for (Map.Entry<String, Class> entry : aliases.entrySet()) {
            xStream.alias(entry.getKey(), entry.getValue());
        }
    }

}
