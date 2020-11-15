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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.BoundFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.parsetree.Type;

public class ProxyRecordWrapperPrototype
	extends ScriptableObject
{
	private static class ContextClass
	{
		public Context context;
		public Class<?> clazz;
		private ContextClass( Context context, Class<?> clazz )
		{
			this.context = context;
			this.clazz = clazz;
		}

		@Override
		public int hashCode()
		{
			return context.hashCode() + clazz.hashCode();
		}

		@Override
		public boolean equals( Object otherObject )
		{
			if ( !( otherObject instanceof ContextClass ) )
			{
				return false;
			}
			ContextClass other = (ContextClass) otherObject;
			return context == other.context && clazz == other.clazz;
		}
	}
	private static Map<ContextClass, ProxyRecordWrapperPrototype> registry = new HashMap<>();

	private Class<?> recordValueClass;

	public ProxyRecordWrapperPrototype( Class<?> recordValueClass )
	{
		this.recordValueClass = recordValueClass;
	}

	public void initToScope( Context cx, Scriptable scope )
	{
		setPrototype( ScriptableObject.getObjectPrototype( scope ) );

		for ( Method method : recordValueClass.getDeclaredMethods() )
		{
			ProxyRecordMethodWrapper methodWrapper = new ProxyRecordMethodWrapper( method );
			String methodShortName = JavascriptRuntime.toCamelCase( method.getName().replace( "get_", "" ) );
			setGetterOrSetter( methodShortName, 0, methodWrapper, false );
		}

		try
		{
			Method constructorMethod = ProxyRecordWrapper.class.getDeclaredMethod( "constructDefaultValue", new Class[] {} );
			FunctionObject constructor = new FunctionObject( getClassName(), constructorMethod, scope );
			constructor.addAsConstructor( scope, this );

			Method getMethod = ProxyRecordWrapper.class.getDeclaredMethod( "genericGet",
				new Class[] { Context.class, Scriptable.class, Object[].class, Function.class } );
			Function getFunction = new FunctionObject( "get", getMethod, scope );
			ScriptableObject.defineProperty( getFunction, "typeName", getClassName(), DONTENUM | READONLY | PERMANENT );
			constructor.defineProperty( "get", getFunction, DONTENUM | READONLY | PERMANENT );

			constructor.sealObject();

			for ( String methodName : new String[] { "toString", "valueOf" } )
			{
				Method method = ProxyRecordWrapper.class.getDeclaredMethod( methodName, new Class[] {} );
				FunctionObject functionObject = new FunctionObject( methodName, method, scope );
				defineProperty( methodName, functionObject, DONTENUM | READONLY | PERMANENT );
				functionObject.sealObject();
			}
		}
		catch ( NoSuchMethodException e )
		{
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ERROR, "NoSuchMethodException: " + e.getMessage() );
		}

		sealObject();

		registry.put( new ContextClass( cx, recordValueClass ), this );
	}

	public static void cleanup( Context cx, Class<?> recordValueClass )
	{
		registry.remove( new ContextClass( cx, recordValueClass ) );
	}

	public static ProxyRecordWrapperPrototype getPrototypeInstance( Context cx, Class<?> recordValueClass )
	{
		return registry.get( new ContextClass( cx, recordValueClass ) );
	}

	public String getClassName()
	{
		return recordValueClass.getSimpleName().replace( "Proxy", "" );
	}
}
