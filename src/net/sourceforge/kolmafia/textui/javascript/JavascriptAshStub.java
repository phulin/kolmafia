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

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.ProxyRecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.Function.MatchType;

public class JavascriptAshStub extends BaseFunction
{
	private ScriptRuntime controller;
	private String ashFunctionName;
	
	public JavascriptAshStub( ScriptRuntime controller, String ashFunctionName )
	{
		this.controller = controller;
		this.ashFunctionName = ashFunctionName;
	}

	@Override
	public String getFunctionName()
	{
		return JavascriptRuntime.toCamelCase( this.ashFunctionName );
	}
	
	@Override
	public Object call( Context cx, Scriptable scope, Scriptable thisObj, Object[] args )
	{
		ValueCoercer coercer = new ValueCoercer( controller, cx, scope );

		// FIXME: Figure out what to do with 0-length arrays.
		// Coerce arguments from Java to ASH values 
		List<Value> ashArgs = new ArrayList<>();
		for ( final Object o : args ) {
			ashArgs.add( coercer.fromJava( o ) );
			if ( ashArgs.get( ashArgs.size() - 1 ) == null )
			{
				throw controller.runtimeException("Argument value is null.");
			}
		}

		// Find library function matching arguments.
		Function function = null;
		Function[] libraryFunctions = RuntimeLibrary.functions.findFunctions( ashFunctionName );

		MatchType[] matchTypes = { MatchType.EXACT, MatchType.BASE, MatchType.COERCE };
		for ( MatchType matchType : matchTypes )
		{
			for ( Function testFunction : libraryFunctions )
			{
				// Check for match with no vararg, then match with vararg.
				if ( testFunction.paramsMatch( ashArgs, matchType, /* vararg = */ false )
					|| testFunction.paramsMatch( ashArgs, matchType, /* vararg = */ true ) )
				{
					function = testFunction;
					break;
				}
			}
			if ( function != null )
			{
				break;
			}
		}

		LibraryFunction ashFunction;
		if ( function instanceof LibraryFunction )
		{
			ashFunction = (LibraryFunction) function;
		}
		else
		{
			throw controller.runtimeException( Parser.undefinedFunctionMessage( ashFunctionName, ashArgs ) );
		}

		List<Object> ashArgsWithInterpreter = new ArrayList<Object>();
		ashArgsWithInterpreter.add(controller);
		ashArgsWithInterpreter.addAll(ashArgs);
		Value ashReturnValue = ashFunction.executeWithoutInterpreter( controller, ashArgsWithInterpreter.toArray() );

		Object returnValue = coercer.asJava( ashReturnValue );

		if ( returnValue instanceof Value && ((Value) returnValue).asProxy() instanceof ProxyRecordValue )
		{
			returnValue = new ProxyRecordWrapper(returnValue.getClass(), (Value) returnValue);
		}
		else if ( !( returnValue instanceof Scriptable ) )
		{
			returnValue = Context.javaToJS(returnValue, scope);
		}

		return returnValue;
	}
}
