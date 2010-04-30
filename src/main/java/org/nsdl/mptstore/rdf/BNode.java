package org.nsdl.mptstore.rdf;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * An RDF BNode.
 *
 * This is a BNode.
 * A <code>BNode</code> can play the part of a subject
 * or object in an RDF triple.
 *
 * @author http://rhizomik.net/~roberto
 */
public class BNode
        implements SubjectNode, ObjectNode {

    /**
     * The BNode local id and the base URI used in order to make the BNode unique across sources.
     */
    private String _bnode;
    private URI _baseURI;
    /**
     * Construct a <code>BNode</code>.
     *
     * @param bnode The BNode id.
     * @param base  The base URI used in order to make BNodes unique across sources
     * @throws URISyntaxException if the base URI is not absolute.
     */
    public BNode(final String bnode, final URI base) throws URISyntaxException {
        if (base.isAbsolute()) {
            _baseURI = base;
            _bnode = bnode;
        } else {
            throw new URISyntaxException(base.toString(), "not absolute");
        }
    }

    /**
     * Construct a <code>BNode</code> given a base URI string.
     *
     * @param bnode The BNode id.
     * @param base  The base URI used in order to make BNodes unique across sources
     * @throws URISyntaxException if the given string is not a valid URI
     *         or is not absolute.
     */
    public BNode(final String bnode, final String base) throws URISyntaxException {
        this(bnode, new URI(base));
    }

    /**
     * Get the BNode local id.
     *
     * @return the BNode local id.
     */
    public String getBNode() {
        return _bnode;
    }
    
    /**
     * Get the base URI.
     *
     * @return the base URI.
     */
    public URI getBaseURI() {
        return _baseURI;
    }

    /** {@inheritDoc} */
    public String getValue() {
        return getUniqueId();
    }

    /** {@inheritDoc} */
    public String toString() {
        return "_:" + getUniqueId();
    }
    
    private String getUniqueId() {
    	return _baseURI.hashCode()+_bnode;
    }

    /** {@inheritDoc} */
    public boolean equals(final Object obj) {
        if (obj != null && obj instanceof BNode) {
            return getUniqueId().equals(((BNode) obj).getUniqueId());
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return getUniqueId().hashCode();
    }

}
