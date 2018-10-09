package org.athento.nx.upgrade.stats;

import org.athento.nx.upgrade.api.GenericInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stat info.
 */
public final class StatInfo implements Serializable {

    String id;
    String name;
    String description;

    List<GenericInfo> entries = new ArrayList<>();

    public StatInfo(String name, String description) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<GenericInfo> getEntries() {
        return entries;
    }

    public void setEntries(List<GenericInfo> entries) {
        this.entries = entries;
    }

    @Override
    public String toString() {
        return "StatInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
