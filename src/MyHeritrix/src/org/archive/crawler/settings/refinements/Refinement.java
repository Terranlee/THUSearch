/* Refinement
 *
 * $Id: Refinement.java 4663 2006-09-25 23:47:38Z paul_jack $
 *
 * Created on Apr 2, 2004
 *
 * Copyright (C) 2004 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.crawler.settings.refinements;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.archive.crawler.settings.CrawlerSettings;
import org.archive.net.UURI;


/**
 * This class acts as a mapping between refinement criterias and a settings
 * object.
 *
 * @author John Erik Halse
 *
 */
public class Refinement {
    private final CrawlerSettings owner;
    private String description;
    private String operator = "Admin";
    private String organization = "";
    private String audience = "";
    private String reference;
    private List<Criteria> criteria = new ArrayList<Criteria>();


    /**
     * Create a new instance of Refinement
     *
     * @param owner the settings object that owns the refinement.
     * @param reference a name that combined with the owner uniquely identifies
     *            the refinement.
     */
    public Refinement(CrawlerSettings owner, String reference) {
        this.owner = owner;
        this.reference = reference;
        owner.addRefinement(this);
    }

    /** Create a new instance of Refinement
     *
     * @param owner the settings object that owns the refinement.
     * @param reference a name that combined with the owner uniquely identifies
     *            the refinement.
     * @param descr A textual description of the refinement.
     */
    public Refinement(CrawlerSettings owner, String reference, String descr) {
        this(owner, reference);
        this.description = descr;
    }

    /**
     * Check if a URI is within the bounds of every criteria set for this
     * refinement.
     *
     * @param uri the URI that shoulb be checked.
     * @return true if within bounds.
     */
    public boolean isWithinRefinementBounds(UURI uri) {
        if (uri == null || uri == null) {
            return false;
        }
        for (Iterator it = criteria.iterator(); it.hasNext();) {
            if (!((Criteria) it.next()).isWithinRefinementBounds(uri)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return the description of this refinement.
     *
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description for this refinement.
     *
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get an <code>ListIterator</code> over the criteria set for this
     * refinement.
     *
     * @return Returns an iterator over the criteria.
     */
    public ListIterator criteriaIterator() {
        return criteria.listIterator();
    }

    /**
     * Add a new criterion to this refinement.
     *
     * @param criterion the criterion to add.
     */
    public void addCriteria(Criteria criterion) {
        if (!criteria.contains(criterion)) {
            criteria.add(criterion);
        }
    }

    /**
     * Get the reference to this refinement's settings object.
     *
     * @return Returns the reference.
     */
    public String getReference() {
        return reference;
    }

    /**
     * Set the reference to this refinement's settings object.
     *
     * @param reference The reference to set.
     */
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Get the <code>CrawlerSettings</code> object this refinement refers to.
     *
     * @return the settings object this refinement refers to.
     */
    public CrawlerSettings getSettings() {
        String parentScope = owner.getScope() == null ? "" : owner.getScope();
        CrawlerSettings settings = owner.getSettingsHandler()
                .getOrCreateSettingsObject(parentScope, getReference());
        settings.setDescription((getDescription()));
        return settings;
    }

    public boolean equals(Object o) {
        if (this == o
                || (o instanceof Refinement && this.reference
                        .equals(((Refinement) o).reference))) {
            return true;
        }
        return false;
    }

    /**
     * @return Returns the audience.
     */
    public String getAudience() {
        return this.audience;
    }
    /**
     * @param audience The audience to set.
     */
    public void setAudience(String audience) {
        this.audience = audience;
    }
    /**
     * @return Returns the operator.
     */
    public String getOperator() {
        return this.operator;
    }
    /**
     * @param operator The operator to set.
     */
    public void setOperator(String operator) {
        this.operator = operator;
    }
    /**
     * @return Returns the organziation.
     */
    public String getOrganization() {
        return this.organization;
    }
    /**
     * @param organization The organziation to set.
     */
    public void setOrganization(String organization) {
        this.organization = organization;
    }
}
