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

package nl.gridshore.cqrs4j.repository.eventsourcing;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import nl.gridshore.cqrs4j.DomainEvent;
import nl.gridshore.cqrs4j.EventStream;
import nl.gridshore.cqrs4j.repository.ObjectInputStreamAdapter;
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
 * Implementation of the {@link nl.gridshore.cqrs4j.repository.eventsourcing.EventStore} that serializes objects using
 * XStream and writes them to files to disk. Each aggregate is represented by a single file, where each event of that
 * aggregate is a line in that file. Events are serialized to XML format, making them readable for both user and
 * machine.
 * <p/>
 * Use {@link #setBaseDir(org.springframework.core.io.Resource)} to specify the directory where event files should be
 * stored
 *
 * @author Allard Buijze
 */
public class XStreamFileSystemEventStore implements EventStore {

    private XStream xStream;
    private Resource baseDir;

    public XStreamFileSystemEventStore() {
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    private Resource getBaseDirForType(String type) {
        try {
            Resource typeSpecificDir = baseDir.createRelative("/" + type + "/");
            if (!typeSpecificDir.exists() && !typeSpecificDir.getFile().mkdirs()) {
                throw new IllegalStateException("The given event store directory doesn't exist and could not be created");
            }
            return typeSpecificDir;
        } catch (IOException e) {
            throw new EventStorageException("An IO Exception occured while reading from the file system", e);
        }
    }

    /**
     * Sets the base directory where the event store will store all events.
     *
     * @param baseDir the location to store event files
     */
    @Required
    public void setBaseDir(Resource baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Specify aliases for classes on serialization. When serializing an object, this event store will use the fully
     * qualified class name as element name. Those are potentially long names. By specifying an alias, they can be
     * considerably shortened.
     *
     * @param aliases a map containing the aliases as keys and their respective class as value
     */
    public void setAliases(Map<String, Class> aliases) {
        for (Map.Entry<String, Class> entry : aliases.entrySet()) {
            xStream.alias(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Specify aliases for package names on serialization. When serializing an object, this event store will use the
     * fully qualified class name as element name. Those are potentially long names. By specifying an alias for a
     * package, they can be considerably shortened.
     *
     * @param aliases a map containing the aliases as keys and the full package name as value
     */
    public void setPackageAliases(Map<String, String> aliases) {
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            xStream.aliasPackage(entry.getKey(), entry.getValue());
        }
    }

}
