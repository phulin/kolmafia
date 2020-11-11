package net.sourceforge.kolmafia.textui.javascript;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
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
		// Coerce arguments from Java to ASH values 
		List<Value> ashArgs = new ArrayList<>();
		for ( final Object o : args ) {
			ashArgs.add( Value.fromJava( o ) );
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
		Value returnValue = ashFunction.executeWithoutInterpreter( controller, ashArgsWithInterpreter.toArray() );

		return Value.asJava( returnValue );
	}
}
