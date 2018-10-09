package org.athento.nx.upgrade.api;

import java.io.Serializable;

/**
 * Generic information class.
 */
public class GenericInfo implements Serializable {

    String name;
    String description;


    public GenericInfo(String name) {
        this.name = name;
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
}
