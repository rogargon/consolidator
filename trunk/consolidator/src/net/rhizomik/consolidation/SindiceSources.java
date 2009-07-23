package net.rhizomik.consolidation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class SindiceSources 
{
	static Logger log = Logger.getLogger("net.rhizomik.consolidation");
	protected static int RESULTS_PAGES = 2;
	
	public static ArrayList<URL> processTxtResult(String query) throws IOException 
	{
		ArrayList<URL> sources = new ArrayList<URL>();
		for (int page=1; page<=RESULTS_PAGES; page++)
		{	
			URL resultsPageURL = new URL(query + "&page=" + page);
			BufferedReader r = new BufferedReader(new InputStreamReader(resultsPageURL.openStream()));
			String line = null;
			while ((line=r.readLine()) != null)
			{
				try
				{
					String urlText = line.substring(0, line.indexOf('\t'));
					URL source = new URL(urlText);
					sources.add(source);
				}
				catch (MalformedURLException e) { log.error(e); }
			}
		}
		return sources;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException 
	{
		ArrayList<URL> sources = SindiceSources.
			processTxtResult(SindiceTermQueryToPajek.sindice1KeywordQuery+"Galway");
		for (URL source: sources)
			System.out.println(source.toString());
	}

}
