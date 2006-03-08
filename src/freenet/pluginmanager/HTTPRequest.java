package freenet.pluginmanager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import freenet.support.URLDecoder;
import freenet.support.URLEncodedFormatException;

/**
 * Used for passing all HTTP request information to the FredPlugin that handles
 * the request. It parses the query string and has several methods for acessing
 * the request parameter values.
 * 
 * @author nacktschneck
 */
public class HTTPRequest {

	/**
	 * This map is used to store all parameter values. The name (as String) of
	 * the parameter is used as key, the returned value is a list (of Strings)
	 * with all values for that parameter sent in the request. You shouldn't
	 * access this map directly, use {@link #getParameterValueList(String)} and
	 * {@link #isParameterSet(String)} insted
	 */
	private final Map parameterNameValuesMap = new HashMap();

	/**
	 * the original URI as given to the constructor
	 */
	private URI uri;

	/**
	 * Create a new HTTPRequest for the given URI and parse its request
	 * parameters.
	 * 
	 * @param uri
	 *            the URI being requested
	 */
	public HTTPRequest(URI uri) {
		this.uri = uri;
		this.parseRequestParameters(uri.getRawQuery(), true);
	}

	/**
	 * Creates a new HTTPRequest for the given path and url-encoded query string
	 * 
	 * @param path i.e. /test/test.html
	 * @param encodedQueryString a=some+text&b=abc%40def.de
	 * @throws URISyntaxException if the URI is invalid
	 */
	public HTTPRequest(String path, String encodedQueryString) throws URISyntaxException {

		if (encodedQueryString!=null && encodedQueryString.length()>0) {
			this.uri = new URI(path+"?"+encodedQueryString);
		} else {
			this.uri = new URI(path);
		}
		this.parseRequestParameters(uri.getRawQuery(), true);
	}

	/**
	 * The path of this request, where the part of the path the specified the
	 * plugin has already been removed..
	 */
	public String getPath() {
		return this.uri.getPath();
	}


	/**
	 * 
	 * @return true if the query string was totally empty
	 */
	public boolean hasParameters() {
		return ! this.parameterNameValuesMap.isEmpty();
	}

	/**
	 * Parse the query string and populate {@link #parameterNameValuesMap} with
	 * the lists of values for each parameter. If this method is not called at
	 * all, all other methods would be useless. Because they rely on the
	 * parameter map to be filled.
	 * 
	 * @param queryString
	 *            the query string in its raw form (not yet url-decoded)
	 * @param doUrlDecoding TODO
	 */
	private void parseRequestParameters(String queryString, boolean doUrlDecoding) {

		// nothing to do if there was no query string in the URI
		if (queryString == null || queryString.length() == 0) {
			return;
		}

		// iterate over all tokens in the query string (seperated by &)
		StringTokenizer tokenizer = new StringTokenizer(queryString, "&");
		while (tokenizer.hasMoreTokens()) {
			String nameValueToken = tokenizer.nextToken();

			// a token can be either a name, or a name value pair...
			String name = null;
			String value = "";
			int indexOfEqualsChar = nameValueToken.indexOf('=');
			if (indexOfEqualsChar < 0) {
				// ...it's only a name, so the value stays emptys
				name = nameValueToken;
			} else if (indexOfEqualsChar == nameValueToken.length() - 1) {
				// ...it's a name with an empty value, so remove the '='
				// character
				name = nameValueToken.substring(0, indexOfEqualsChar);
			} else {
				// ...it's a name value pair, split into name and value
				name = nameValueToken.substring(0, indexOfEqualsChar);
				value = nameValueToken.substring(indexOfEqualsChar + 1);
			}

			// url-decode the name and value
			try {
				if (doUrlDecoding) {
					name = URLDecoder.decode(name);
					value = URLDecoder.decode(value);
				}

				// get the list of values for this parameter that were parsed so
				// far
				List valueList = this.getParameterValueList(name);
				// add this value to the list
				valueList.add(value);
			} catch (URLEncodedFormatException e) {
				// if we fail to decode the name or value we fail spectacularly
				String msg = "Failed to decode request parameter " + name
						+ " with value '" + value + "'";
				throw new RuntimeException(msg, e);
			}
		}
	}

	/**
	 * Get the first value of the parameter with the given name.
	 * 
	 * @param name
	 *            the name of the parameter to get
	 * @return the first value or <code>null</code> if the parameter was not
	 *         set
	 */
	private String getParameterValue(String name) {
		if (!this.isParameterSet(name)) {
			return null;
		}
		List allValues = this.getParameterValueList(name);
		return (String) allValues.get(0);
	}

	/**
	 * Get the list of all values for the parameter with the given name. When
	 * this method is called for a given parameter for the first time, a new
	 * empty list of values is created and stored in
	 * {@link #parameterNameValuesMap}. This list is returned and should be
	 * used to add parameter values. If you only want to check if a parameter is
	 * set at all, you must use {@link #isParameterSet(String)}.
	 * 
	 * @param name
	 *            the name of the parameter to get
	 * @return the list of all values for this parameter that were parsed so
	 *         far.
	 */
	private List getParameterValueList(String name) {
		List values = (List) this.parameterNameValuesMap.get(name);
		if (values == null) {
			values = new LinkedList();
			this.parameterNameValuesMap.put(name, values);
		}
		return values;
	}

	/**
	 * Check if a parameter was set in the request at all, either with or
	 * without a value.
	 * 
	 * @param name
	 *            the name of the parameter to check
	 * @return true if the parameter was set in the request, not regarding if
	 *         the value is empty
	 */
	public boolean isParameterSet(String name) {
		return this.parameterNameValuesMap.containsKey(name);
	}

	/**
	 * Get the value of a request parameter, using an empty string as default
	 * value if the parameter was not set. This method will never return null,
	 * so its safe to do things like
	 * 
	 * <p>
	 * <code>
	 *   if (request.getParam(&quot;abc&quot;).equals(&quot;def&quot;))
	 * </code>
	 * </p>
	 * 
	 * @param name
	 *            the name of the parameter to get
	 * @return the parameter value as String, or an empty String if the value
	 *         was missing or empty
	 */
	public String getParam(String name) {
		return this.getParam(name, "");
	}

	/**
	 * Get the value of a request parameter, using the specified default value
	 * if the parameter was not set or has an empty value.
	 * 
	 * @param name
	 *            the name of the parameter to get
	 * @param defaultValue
	 *            the default value to be returned if the parameter is missing
	 *            or empty
	 * @return either the parameter value as String, or the default value
	 */
	public String getParam(String name, String defaultValue) {
		String value = this.getParameterValue(name);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}

	/**
	 * Get the value of a request parameter converted to an int, using 0 as
	 * default value. If there are multiple values for this parameter, the first
	 * value is used.
	 * 
	 * @param name
	 *            the name of the parameter to get
	 * @return either the parameter value as int, or 0 if the parameter is
	 *         missing, empty or invalid
	 */
	public int getIntParam(String name) {
		return this.getIntParam(name, 0);
	}

	/**
	 * Get the value of a request parameter converted to an <code>int</code>,
	 * using the specified default value. If there are multiple values for this
	 * parameter, the first value is used.
	 * 
	 * @param name
	 *            the name of the parameter to get
	 * @param defaultValue
	 *            the default value to be returned if the parameter is missing,
	 *            empty or invalid
	 * @return either the parameter value as int, or the default value
	 */
	public int getIntParam(String name, int defaultValue) {
		if (!this.isParameterSet(name)) {
			return defaultValue;
		}
		String value = this.getParameterValue(name);
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	// TODO: add similar methods for long, boolean etc.

	/**
	 * Get all values of a request parameter as a string array. If the parameter
	 * was not set at all, an empty array is returned, so this method will never
	 * return <code>null</code>.
	 * 
	 * @param name
	 *            the name of the parameter to get
	 * @return an array of all paramter values that might include empty values
	 */
	public String[] getMultipleParam(String name) {
		List valueList = this.getParameterValueList(name);
		String[] values = new String[valueList.size()];
		valueList.toArray(values);
		return values;
	}

	/**
	 * Get all values of a request parameter as int array, ignoring all values
	 * that can not be parsed. If the parameter was not set at all, an empty
	 * array is returned, so this method will never return <code>null</code>.
	 * 
	 * @param name
	 *            the name of the parameter to get
	 * @return an int array of all parameter values that could be parsed as int
	 */
	public int[] getMultipleIntParam(String name) {
		List valueList = this.getParameterValueList(name);

		// try parsing all values and put the valid Integers in a new list
		List intValueList = new ArrayList();
		for (int i = 0; i < valueList.size(); i++) {
			try {
				intValueList.add(new Integer((String) valueList.get(i)));
			} catch (Exception e) {
				// ignore invalid parameter values
			}
		}

		// convert the valid Integers to an array of ints
		int[] values = new int[intValueList.size()];
		for (int i = 0; i < intValueList.size(); i++) {
			values[i] = ((Integer) intValueList.get(i)).intValue();
		}
		return values;
	}


	// TODO: add similar methods for multiple long, boolean etc.

}
