/**
 * Copyright (c) 2005-2018, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.io.File;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.RelayRequest;

import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.NamespaceInterpreter;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;

import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;

public abstract class KoLmafiaASH
{
	private static final HashMap<String, File> relayScriptMap = new HashMap<String, File>();

	private static final HashMap<File, Long> TIMESTAMPS = new HashMap<File, Long>();
	private static final HashMap<File, Interpreter> INTERPRETERS = new HashMap<File, Interpreter>();

	public static final Interpreter NAMESPACE_INTERPRETER = new NamespaceInterpreter();

	public static final void logScriptExecution( final String prefix, final String scriptName, Interpreter script )
	{
		KoLmafiaASH.logScriptExecution( prefix, scriptName, "", script );
	}

	public static final void logScriptExecution( final String prefix, final String scriptName, final String postfix, Interpreter script )
	{
		boolean isDebugging = RequestLogger.isDebugging();
		boolean isTracing = RequestLogger.isTracing();
		boolean scriptIsTracing = Interpreter.isTracing();

		if ( !isDebugging && !isTracing && !scriptIsTracing )
		{
			return;
		}

		String message = prefix + scriptName + postfix;

		if ( isDebugging )
		{
			RequestLogger.updateDebugLog( message );
		}

		if ( isTracing )
		{
			RequestLogger.trace( message );
		}

 		if ( scriptIsTracing )
 		{
			script.trace( message );
 		}
	}

	public static final boolean getClientHTML( final RelayRequest request )
	{
		String script = request.getBasePath();
		String field = null;
		String alternateField = null;

		if ( script.equals( "place.php" ) )
		{
			field = request.getFormField( "whichplace" );
		}
		else if ( script.equals( "shop.php" ) )
		{
			field = request.getFormField( "whichshop" );
		}
		else if ( script.equals( "campground.php" ) )
		{
			field = request.getFormField( "action" );
			if ( field != null && field.equals( "workshed" ) )
			{
				AdventureResult workshed_item = CampgroundRequest.getCurrentWorkshedItem();
				if ( workshed_item != null )
				{
					alternateField = field + "." + workshed_item.getItemId();
				}
			}
		}

		if ( alternateField != null )
		{
			String fullscript = script.substring( 0, script.length() - 4 ) + "." + alternateField + ".ash";
			File toExecute;
			if ( KoLmafiaASH.relayScriptMap.containsKey( fullscript ) )
			{
				toExecute = KoLmafiaASH.relayScriptMap.get( fullscript );
			}
			else
			{
				toExecute = new File( KoLConstants.RELAY_LOCATION, fullscript );
				KoLmafiaASH.relayScriptMap.put( fullscript, toExecute );
			}
			if ( toExecute.exists() )
			{
				return KoLmafiaASH.getClientHTML( request, toExecute );
			}
		}

		if ( field != null )
		{
			// This block and the alternateField block above are supposed to be the same.
			// There's probably a simple way of refactoring for simplicity, but until then,
			// any changes to one block should be done in the other.
			String fullscript = script.substring( 0, script.length() - 4 ) + "." + field + ".ash";
			File toExecute;
			if ( KoLmafiaASH.relayScriptMap.containsKey( fullscript ) )
			{
				toExecute = KoLmafiaASH.relayScriptMap.get( fullscript );
			}
			else
			{
				toExecute = new File( KoLConstants.RELAY_LOCATION, fullscript );
				KoLmafiaASH.relayScriptMap.put( fullscript, toExecute );
			}
			if ( toExecute.exists() )
			{
				return KoLmafiaASH.getClientHTML( request, toExecute );
			}
		}

		if ( KoLmafiaASH.relayScriptMap.containsKey( script ) )
		{
			File toExecute = KoLmafiaASH.relayScriptMap.get( script );
			return toExecute.exists() && KoLmafiaASH.getClientHTML( request, toExecute );
		}

		if ( !script.endsWith( ".ash" ) )
		{
			if ( !script.endsWith( ".php" ) )
			{
				return false;
			}

			script = script.substring( 0, script.length() - 4 ) + ".ash";
		}

		File toExecute = new File( KoLConstants.RELAY_LOCATION, script );
		KoLmafiaASH.relayScriptMap.put( script, toExecute );
		return toExecute.exists() && KoLmafiaASH.getClientHTML( request, toExecute );
	}

	private static final boolean getClientHTML( final RelayRequest request, final File toExecute )
	{
		Interpreter relayScript = KoLmafiaASH.getInterpreter( toExecute );
		if ( relayScript == null )
		{
			return false;
		}

		synchronized ( relayScript )
		{
			// We are synchronized, so no other thread is in this
			// relay script, but this thread could be inside it: if
			// KoL redirects to the same page (but with different
			// arguments), the same script will want to handle the
			// redirection.

			if ( relayScript.getRelayRequest() != null )
			{
				return false;
			}

			KoLmafiaASH.logScriptExecution( "Starting relay script: ", toExecute.getName(), "", relayScript );

			RelayRequest relayRequest = new RelayRequest( false );
			relayRequest.cloneURLString( request );

			relayScript.initializeRelayScript( relayRequest );

			relayScript.execute( "main", null );

			StringBuffer serverReplyBuffer = relayScript.getServerReplyBuffer();

			if ( serverReplyBuffer.length() == 0 )
			{
				if ( relayRequest.responseText != null && relayRequest.responseText.length() != 0 )
				{
					serverReplyBuffer.append( relayRequest.responseText );
				}
			}

			int written = serverReplyBuffer.length();
			if ( written != 0 )
			{
				String response = serverReplyBuffer.toString();
				request.pseudoResponse( "HTTP/1.1 200 OK", response );
			}

			relayScript.finishRelayScript();

			KoLmafiaASH.logScriptExecution( "Finished relay script: ", toExecute.getName(), " (" + written + " bytes)",  relayScript );

			return written != 0;
		}
	}

	// Convenience method so that callers can just do getInterpreter( KoLMafiaCLI.findScriptFile() )
	public static Interpreter getInterpreter( List<File> findScriptFile )
	{
		if ( findScriptFile.size() > 1 )
		{
			RequestLogger.printList( findScriptFile );
			RequestLogger.printLine( "Multiple matching scripts in your current namespace." );
			return null;
		}
		if ( findScriptFile.size() == 1 )
			return getInterpreter( findScriptFile.get( 0 ) );

		return null;
	}

	public static final Interpreter getInterpreter( final File toExecute )
	{
		if ( toExecute == null )
		{
			return null;
		}

		boolean createInterpreter = !KoLmafiaASH.TIMESTAMPS.containsKey( toExecute );

		if ( !createInterpreter )
		{
			Long timestamp = KoLmafiaASH.TIMESTAMPS.get( toExecute );
			createInterpreter = timestamp != toExecute.lastModified();
		}

		if ( !createInterpreter )
		{
			Interpreter interpreter = (Interpreter) KoLmafiaASH.INTERPRETERS.get( toExecute );
			TreeMap imports = interpreter.getImports();

			Iterator it = imports.entrySet().iterator();

			while ( it.hasNext() && !createInterpreter )
			{
				Entry entry = (Entry) it.next();
				File file = (File) entry.getKey();
				Long timestamp = (Long) entry.getValue();
				createInterpreter = timestamp != file.lastModified();
			}
		}

		if ( createInterpreter )
		{
			KoLmafiaASH.TIMESTAMPS.remove( toExecute );
			Interpreter interpreter = new Interpreter();

			if ( !interpreter.validate( toExecute, null ) )
			{
				return null;
			}

			KoLmafiaASH.TIMESTAMPS.put( toExecute, toExecute.lastModified() );
			KoLmafiaASH.INTERPRETERS.put( toExecute, interpreter );
		}

		return KoLmafiaASH.INTERPRETERS.get( toExecute );
	}

	public static void showUserFunctions( final Interpreter interpreter, final String filter )
	{
		KoLmafiaASH.showFunctions( interpreter.getFunctions(), filter.toLowerCase(), false );
	}

	public static void showExistingFunctions( final String filter )
	{
		KoLmafiaASH.showFunctions( RuntimeLibrary.getFunctions(), filter.toLowerCase(), true );
	}

	private static void showFunctions( final FunctionList functions, final String filter, boolean addLinks )
	{
		addLinks = addLinks && StaticEntity.isGUIRequired();

		if ( functions.isEmpty() )
		{
			RequestLogger.printLine( "No functions in your current namespace." );
			return;
		}

		for ( Function func : functions )
		{
			boolean matches = filter.equals( "" );

			if ( !matches )
			{
				matches = func.getName().toLowerCase().contains( filter );
			}

			if ( !matches )
			{
				for ( VariableReference ref : func.getVariableReferences() )
				{
					String refType = ref.getType().toString();
					matches = refType != null && refType.contains( filter );
				}
			}

			if ( !matches )
			{
				continue;
			}

			StringBuilder description = new StringBuilder();

			description.append( func.getType() );
			description.append( " " );
			if ( addLinks )
			{
				description.append( "<a href='https://wiki.kolmafia.us/index.php?title=" );
				description.append( func.getName() );
				description.append( "'>" );
			}
			description.append( func.getName() );
			if ( addLinks )
			{
				description.append( "</a>" );
			}
			description.append( "( " );

			String sep = "";
			for ( VariableReference var : func.getVariableReferences() )
			{
				description.append( sep );
				sep = ", ";

				description.append( var.getType() );

				if ( var.getName() != null )
				{
					description.append( " " );
					description.append( var.getName() );
				}
			}

			description.append( " )" );

			RequestLogger.printLine( description.toString() );

		}
	}

	public static final void stopAllRelayInterpreters()
	{
		for ( Interpreter i : KoLmafiaASH.INTERPRETERS.values() )
		{
			if ( i.getRelayRequest() != null )
			{
				i.setState( Interpreter.STATE_EXIT );
			}
		}
	}
}
