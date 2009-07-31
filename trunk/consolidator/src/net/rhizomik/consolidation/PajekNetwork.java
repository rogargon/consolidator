package net.rhizomik.consolidation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.nsdl.mptstore.rdf.Triple;
import org.nsdl.mptstore.util.NTriplesUtil;

/**
 * @author http://rhizomik.net/~roberto
 *
 * Transform a set of RDF triples into a Pajek Network file
 */

public class PajekNetwork
{
	private static String NETWORK = "*network ";
	private static String VERTICES = "*vertices ";
	private static String ARCS = "*arcs";
	private static int EXPLICIT = 1;
	private static int INFERRED = 2;

	private String networkName;
	private Hashtable<String, VertexInfo> network;
	private int lastVertexID;
	
    private class VertexInfo
	{
		public int vertexID;
		public Set<ArcInfo> neighbours;
		VertexInfo(int vertexID)
		{
			this.vertexID = vertexID;
			this.neighbours = new HashSet<ArcInfo>();
		}
	}
    
    private class ArcInfo
    {
    	public int targetVertexID;
    	public int arcType;
    	public String arcLabel;
    	ArcInfo (String arcLabel, int targetVertexID, int arcType)
    	{
    		this.arcLabel = arcLabel;
    		this.targetVertexID = targetVertexID;
    		this.arcType = arcType;
    	}
    }
	
	/**
     * @param networkName
     */
    PajekNetwork(String networkName)
	{
		this.networkName = networkName;
		this.network = new Hashtable<String, VertexInfo>();
		this.lastVertexID = 1;
	}
	
	/**
     * @param triple
     */
    public void addArc(String triple, String baseURI, boolean inferred) throws ParseException
    {
		Triple t = NTriplesUtil.parseTriple(triple, baseURI);
		
		String v1Label = t.getSubject().getValue().replace('\n', ' ');
		String arcLabel = t.getPredicate().getValue().replace('\n', ' ');
		String v2Label = t.getObject().getValue().replace('\n', ' ').replace('\"', '\'');
		if (!inferred)
			addArc(v1Label, arcLabel, v2Label, EXPLICIT);
		else
			addArc(v1Label, arcLabel, v2Label, INFERRED);
    }
    
	/**
	 * Add an arc between two nodes to the Pajek Network
	 * data structure. The corresponding nodes are added if
	 * they are not present.
     * @param v1ID
     * @param v2ID
     */
    private void addArc(String v1Label, String arcLabel, String v2Label, int arcType)
    {
    	int v2ID;
    	VertexInfo v1Info;
    	
        if (!network.containsKey(v1Label))
        {
           	v1Info = new VertexInfo(lastVertexID++);
			network.put(v1Label, v1Info);
        }
        else
        	v1Info = network.get(v1Label);
		if (!network.containsKey(v2Label))
		{
			v2ID = lastVertexID++;
			network.put(v2Label, new VertexInfo(v2ID));
		}
		else
			v2ID = network.get(v2Label).vertexID;
			
		v1Info.neighbours.add(new ArcInfo(arcLabel, v2ID, arcType));
    }

    /**
     * Serialise the Pajek Network datastructure in 
     * Pajek .net file format
     * @param writer
     * @throws IOException
     */
    public void serialisePajek(Writer writer) throws IOException
	{
		PrintWriter printer = new PrintWriter(writer);
		printer.print(NETWORK+networkName+"\r\n");
		
		printer.print(VERTICES+network.size()+"\r\n");
		
		for(Enumeration<String> e = network.keys(); e.hasMoreElements();)
		{
			String label = e.nextElement();
			int ID = network.get(label).vertexID;
			printer.print(ID+" \""+label+"\""+"\r\n");
		}
		writer.flush();
		printer.print(ARCS+"\r\n");
		for(Enumeration<String> e = network.keys(); e.hasMoreElements();)
		{
			String label = e.nextElement();
			VertexInfo info = network.get(label); 
			int ID = info.vertexID;
			for(ArcInfo arc: info.neighbours)
			{
				printer.print(ID+" "+arc.targetVertexID+" "+arc.arcType+" l "+arc.arcLabel+"\r\n");
			}
		}
		writer.flush();
		writer.close();
	}
}
