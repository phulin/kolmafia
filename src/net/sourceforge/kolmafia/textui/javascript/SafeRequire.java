package net.sourceforge.kolmafia.textui.javascript;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataFileCache;
import net.sourceforge.kolmafia.textui.ScriptException;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.UserDefinedFunction;

public class SafeRequire
        extends Require
{
        private static final long serialVersionUID = 1L;

        public SafeRequire( Context cx, Scriptable nativeScope )
        {
                super( cx, nativeScope, new SoftCachingModuleScriptProvider( new UrlModuleSourceProvider( Arrays.asList( KoLConstants.ROOT_LOCATION.toURI() ), null ) ), null, null, true );
        }

        @Override
        public Object call( Context cx, Scriptable scope, Scriptable thisObj, Object[] args )
        {
                if ( args == null || args.length < 1 || !(args[0] instanceof String) )
                {
                        throw new ScriptException( "require() needs one argument, a string" );
                }

                String path = (String) args[0];
                if ( path.endsWith( ".ash" ) )
                {
                        Scriptable exports = cx.newObject( scope );

                        List<File> scriptFiles = KoLmafiaCLI.findScriptFile( path );
                        List<File> validScriptFiles = scriptFiles.stream().filter( f -> {
                                try
                                {
                                        return f.getCanonicalPath().startsWith( KoLConstants.ROOT_LOCATION.getCanonicalPath() );
                                }
                                catch ( IOException e )
                                {
                                        KoLmafia.updateDisplay( MafiaState.ERROR, "Could not resolve path " + f.getPath() );
                                        return false;
                                }
                        } ).collect( Collectors.toList() );
                        AshRuntime interpreter = (AshRuntime) KoLmafiaASH.getInterpreter( validScriptFiles );

                        if ( interpreter == null )
                        {
                                throw new ScriptException( "Module \"" + path + "\" not found." );
                        }

                        for ( Function f : interpreter.getFunctions() )
                        {
                                UserDefinedFunction userDefinedFunction = (UserDefinedFunction) f;
                                UserDefinedFunctionStub stub = new UserDefinedFunctionStub( interpreter, userDefinedFunction.getName() );
                                ScriptableObject.putProperty( exports, JavascriptRuntime.toCamelCase( userDefinedFunction.getName() ), stub );
                        }

                        interpreter.execute( null, null );

                        return exports;
                }
                else
                {
                        File file = DataFileCache.getFile( path, /* readOnly = */ true );
                        if ( file == null )
                        {
                                throw new ScriptException( "Module \"" + path + "\" not found." );
                        }

                        return super.call( cx, scope, thisObj, new String[] {file.toURI().toString()} );
                }
        }
}
