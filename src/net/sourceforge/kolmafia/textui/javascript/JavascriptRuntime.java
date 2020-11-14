/**
 * Copyright (c) 2005-2020, KoLmafia development team
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

package net.sourceforge.kolmafia.textui.javascript;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.textui.parsetree.ProxyRecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.ScriptException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class JavascriptRuntime
	implements ScriptRuntime
{
	static Set<JavascriptRuntime> runningRuntimes = new HashSet<>();

	private File scriptFile;

	private State runtimeState = State.EXIT;

	// For relay scripts.
	private RelayRequest relayRequest = null;
	private StringBuffer serverReplyBuffer = null;

	private LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched;
	
	public static String toCamelCase( String name )
	{
		if ( name == null )
		{
			return null;
		}

		boolean first = true;
		StringBuilder result = new StringBuilder();
		for ( String word : name.split( "_" ) )
		{
			if ( first )
			{
				result.append( word.charAt( 0 ) );
				first = false;
			}
			else
			{
				result.append( Character.toUpperCase( word.charAt( 0 ) ) );
			}
			result.append( word.substring( 1 ) );
		}

		return result.toString();
	}
	
	public JavascriptRuntime( File scriptFile )
	{
		this.scriptFile = scriptFile;
	}
	
	private void initRuntimeLibrary( Context cx, Scriptable scope )
	{
		Scriptable stdLib = cx.newObject(scope);
		Set<String> functionNameSet = new TreeSet<>();
		for ( net.sourceforge.kolmafia.textui.parsetree.Function libraryFunction : RuntimeLibrary.functions )
		{
			// Blacklist a number of types.
			List<Type> allTypes = new ArrayList<>();
			allTypes.add( libraryFunction.getType() );
			for ( VariableReference variableReference : libraryFunction.getVariableReferences() )
			{
				allTypes.add( variableReference.getType() );
			}
			if ( allTypes.contains( DataTypes.MATCHER_TYPE ) ) continue;

			functionNameSet.add(libraryFunction.getName());
		}
		for ( String libraryFunctionName : functionNameSet )
		{
			ScriptableObject.putProperty( stdLib, toCamelCase( libraryFunctionName ),
				new JavascriptAshStub( this, libraryFunctionName ) );
		}
		ScriptableObject.putProperty( scope, "Lib", stdLib );
	}

	private static void initProxyRecordValueType( Context cx, Scriptable scope, Class<?> recordValueClass )
	{
		ProxyRecordWrapperPrototype prototype = new ProxyRecordWrapperPrototype( recordValueClass );
		prototype.initToScope( cx, scope );
	}

	private static void initProxyRecordValueTypes( Context cx, Scriptable scope )
	{
		for ( Class<?> proxyRecordValueClass : ProxyRecordValue.class.getDeclaredClasses() )
		{
			if ( !proxyRecordValueClass.getSimpleName().endsWith("Proxy") )
			{
				continue;
			}
			initProxyRecordValueType( cx, scope, proxyRecordValueClass );
		}
	}

	private static void cleanupProxyRecordValueTypes( Context cx )
	{
		for ( Class<?> proxyRecordValueClass : ProxyRecordValue.class.getDeclaredClasses() )
		{
			if ( !proxyRecordValueClass.getSimpleName().endsWith( "Proxy" ) )
			{
				continue;
			}
			ProxyRecordWrapperPrototype.cleanup( cx, proxyRecordValueClass );
		}
	}

	public void execute()
	{
		Context cx = Context.enter();
		cx.setLanguageVersion( Context.VERSION_ES6 );
		runningRuntimes.add( this );

		try
		{
			Scriptable scope = cx.initSafeStandardObjects();

			initRuntimeLibrary(cx, scope);
			initProxyRecordValueTypes(cx, scope);

			FileReader scriptFileReader = new FileReader( scriptFile );

			try
			{
				setState(State.NORMAL);
				cx.evaluateReader( scope, scriptFileReader, scriptFile.getName(), 0, null );
				Object mainFunction = scope.get( "main", scope );
				if ( mainFunction instanceof Function )
				{
					Object result = ( (Function) mainFunction ).call( cx, scope, cx.newObject(scope), null );
					System.out.println( Context.jsToJava( result, boolean.class ) );
				}
			}
			catch ( EvaluatorException e )
			{
				KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "JavaScript evaluator exception: " + e.getMessage() + "\n" + e.getScriptStackTrace() );
			}
			catch ( EcmaError e )
			{
				KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "JavaScript error: " + e.getErrorMessage() + "\n" + e.getScriptStackTrace() ); 
			}
			catch ( JavaScriptException e )
			{
				KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "JavaScript exception: " + e.getMessage() + "\n" + e.getScriptStackTrace() );
			}
			catch ( ScriptException e )
			{
				KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "Script exception: " + e.getMessage() );
			}
			finally
			{
				setState(State.EXIT);
				scriptFileReader.close();
			}
		}
		catch ( FileNotFoundException e )
		{
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "File not found" );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "JS file I/O error" );
		}
		finally
		{
			runningRuntimes.remove( this );
			cleanupProxyRecordValueTypes( cx );
			Context.exit();
		}
	}

	@Override
	public ScriptException runtimeException( final String message )
	{
		return new ScriptException( Context.reportRuntimeError(message).getMessage() );
	}

	@Override
	public ScriptException runtimeException2(final String message1, final String message2)
	{
		return new ScriptException( Context.reportRuntimeError(message1 + " " + message2).getMessage() );
	}

	@Override
	public RelayRequest getRelayRequest()
	{
		return relayRequest;
	}

	@Override
	public StringBuffer getServerReplyBuffer()
	{
		return serverReplyBuffer;
	}

	@Override
	public State getState()
	{
		return runtimeState;
	}

	@Override
	public void setState(final State newState)
	{
		runtimeState = newState;
	}

	@Override
	public LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> getBatched()
	{
		return batched;
	}

	@Override
	public void setBatched( LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched )
	{
		this.batched = batched;
	}
}