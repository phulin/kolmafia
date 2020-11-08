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
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.RuntimeController;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.ScriptException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeSet;

public class JavascriptRuntime implements RuntimeController
{
	static Set<JavascriptRuntime> runningRuntimes = new HashSet<>();

	private File scriptFile;

	private State runtimeState = State.EXIT;

    // For relay scripts.
	private RelayRequest relayRequest = null;
	private StringBuffer serverReplyBuffer = null;

	private LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched;
	
	public JavascriptRuntime( File scriptFile )
	{
		this.scriptFile = scriptFile;
	}
    
    public void execute()
    {
        Context cx = Context.enter();

        try
        {
            Scriptable scope = cx.initSafeStandardObjects();

            FileReader f = new FileReader( scriptFile );

			Scriptable stdLib = cx.newObject(scope);
			Set<String> functionNameSet = new TreeSet<>();
			for ( Function libraryFunction : RuntimeLibrary.functions )
			{
				functionNameSet.add(libraryFunction.getName());
			}
			for ( String libraryFunctionName : functionNameSet )
			{
				ScriptableObject.putProperty( stdLib, libraryFunctionName, new JavascriptAshStub( this, libraryFunctionName ) );
			}
            ScriptableObject.putProperty( scope, "ash", stdLib );

            try
            {
				setState(State.NORMAL);
                cx.evaluateReader( scope, f, scriptFile.getName(), 0, null );
				Object mainFunction = scope.get( "main", scope );
				if (mainFunction instanceof org.mozilla.javascript.Function)
				{
					Object result = ( (org.mozilla.javascript.Function) mainFunction ).call( cx, scope, cx.newObject(scope), null );
					System.out.println( Context.jsToJava( result, boolean.class ) );
				}
            }
            catch ( EvaluatorException e )
            {
                KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "JavaScript evaluator exception.\n" + e.getScriptStackTrace() );
            }
            finally
            {
				setState(State.EXIT);
                f.close();
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
            Context.exit();
        }
    }

    @Override
	public ScriptException runtimeException( final String message )
    {
        return new ScriptException(
			Context.reportRuntimeError(message).getMessage()
		);
    }

    @Override
    public ScriptException runtimeException2(final String message1, final String message2)
    {
        return new ScriptException(
			Context.reportRuntimeError(message1 + " " + message2).getMessage()
		);
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