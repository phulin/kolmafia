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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.parsetree.ProxyRecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;

public class ProxyRecordWrapper
	extends ScriptableObject
{
	private Class<?> recordValueClass;
	// NB: This wrapped value is NOT the proxy record type version.
	// Instead, it's the plain value that can be turned into a proxy record via asProxy.
	private Value wrapped;

	public ProxyRecordWrapper( Class<?> recordValueClass, Value wrapped )
	{
		this.recordValueClass = recordValueClass;
		this.wrapped = wrapped;
		setPrototype( ProxyRecordWrapperPrototype.getPrototypeInstance( Context.getCurrentContext(), recordValueClass ) );
		sealObject();
	}

	public Value getWrapped()
	{
		return wrapped;
	}

	@Override
	public String getClassName()
	{
		return recordValueClass.getName();
	}

	@Override
	public String toString()
	{
		return wrapped.toString();
	}

	public long valueOf()
	{
		return wrapped.contentLong;
	}

	public static Object constructDefaultValue()
	{
		return new ProxyRecordWrapper( ProxyRecordValue.ItemProxy.class,
			new ProxyRecordValue.ItemProxy( DataTypes.makeIntValue( 1 ) ) );
	}

	private static ProxyRecordWrapper getOne( Type type, Object key )
	{
		Value rawValue = type.initialValue();
		if ( key instanceof String )
		{
			rawValue = type.parseValue( (String) key, true );
		}
		else if ( key instanceof Float || key instanceof Double )
		{
			rawValue = type.makeValue( (int) Math.round( (Double) key ), true );
		}
		else if ( key instanceof Number )
		{
			rawValue = type.makeValue( ((Number) key).intValue(), true );
		}

		Class<?> proxyRecordValueClass = null;
		for ( Class<?> testRecordValueClass : ProxyRecordValue.class.getDeclaredClasses() )
		{
			if ( testRecordValueClass.getSimpleName().toLowerCase().startsWith( type.getName() ) )
			{
				proxyRecordValueClass = testRecordValueClass;
			}
		}

		return new ProxyRecordWrapper( proxyRecordValueClass, rawValue );
	}

	public static Object genericGet( Context cx, Scriptable thisObject, Object[] args, Function functionObject )
	{
		if ( args.length != 1 )
		{
			return Undefined.instance;
		}

		String typeName = (String) ScriptableObject.getProperty( functionObject, "typeName" );
		Type type = DataTypes.simpleTypes.find( typeName );

		Object arg = args[0];
		if ( arg instanceof Iterable )
		{
			List<Object> result = new ArrayList<>();
			for ( Object key : (Iterable<?>) arg )
			{
				result.add( getOne( type, key ) );
			}
			return new NativeArray( result.toArray() );
		}
		else
		{
			return getOne( type, arg );
		}
	}
}
