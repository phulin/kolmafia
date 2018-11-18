/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.wc;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

/**
 * The <b>SVNWCUtil</b> is a utility class providing some common methods used
 * by Working Copy API classes for such purposes as creating default run-time
 * configuration and authentication drivers and some others.
 * 
 * 
 * @version 1.3
 * @author TMate Software Ltd., Peter Skoog
 * @since  1.2
 * @see ISVNOptions
 * @see <a target="_top" href="http://svnkit.com/kb/examples/">Examples</a>
 */
public class SVNWCUtil {

    private static final String ECLIPSE_AUTH_MANAGER_CLASSNAME = "org.tmatesoft.svn.core.internal.wc.EclipseSVNAuthenticationManager";
    private static Boolean ourIsEclipse;

    /**
     * Gets the location of the default SVN's run-time configuration area on the
     * current machine. The result path depends on the platform on which SVNKit
     * is running:
     * <ul>
     * <li>on <i>Windows</i> this path usually looks like <i>'Documents and
     * Settings\UserName\Subversion'</i> or simply <i>'%APPDATA%\Subversion'</i>.
     * <li>on a <i>Unix</i>-like platform - <i>'~/.subversion'</i>.
     * </ul>
     * 
     * @return a {@link java.io.File} representation of the default SVN's
     *         run-time configuration area location
     */
    public static File getDefaultConfigurationDirectory() {
        if (SVNFileUtil.isWindows && !SVNFileUtil.isOS2) {
            return new File(SVNFileUtil.getApplicationDataPath(), "Subversion");
        } else if (SVNFileUtil.isOpenVMS) {
            return new File("/sys$login", ".subversion").getAbsoluteFile();
        }
        return new File(System.getProperty("user.home"), ".subversion");
    }

    /**
     * Creates a default authentication manager that uses the default SVN's
     * <i>servers</i> configuration and authentication storage. Whether the
     * default auth storage is used or not depends on the 'store-auth-creds'</i>
     * option that can be found in the SVN's <i>config</i> file under the
     * <i>[auth]</i> section.
     * 
     * @return a default implementation of the credentials and servers
     *         configuration driver interface
     * @see #getDefaultConfigurationDirectory()
     */
    public static ISVNAuthenticationManager createDefaultAuthenticationManager() {
        return createDefaultAuthenticationManager(getDefaultConfigurationDirectory(), null, (char[]) null);
    }

    /**
     * Creates a default authentication manager that uses the <i>servers</i>
     * configuration and authentication storage located in the provided
     * directory. The authentication storage is enabled.
     * 
     * @param configDir
     *            a new location of the run-time configuration area
     * @return a default implementation of the credentials and servers
     *         configuration driver interface
     */
    public static ISVNAuthenticationManager createDefaultAuthenticationManager(File configDir) {
        return createDefaultAuthenticationManager(configDir, null, (char[]) null, true);
    }

    /**
     * Creates a default authentication manager that uses the default SVN's
     * <i>servers</i> configuration and provided user's credentials. Whether
     * the default auth storage is used or not depends on the 'store-auth-creds'</i>
     * option that can be found in the SVN's <i>config</i> file under the
     * <i>[auth]</i> section.
     * 
     * @param userName
     *            a user's name
     * @param password
     *            a user's password
     * @return a default implementation of the credentials and servers
     *         configuration driver interface
     *         
     * @since 1.8.9
     */
    public static ISVNAuthenticationManager createDefaultAuthenticationManager(String userName, char[] password) {
        return createDefaultAuthenticationManager(null, userName, password);
    }

    /**
     * @deprecated Use {@link #createDefaultAuthenticationManager(String, char[])} method.
     */
    public static ISVNAuthenticationManager createDefaultAuthenticationManager(String userName, String password) {
        return createDefaultAuthenticationManager(null, userName, password);
    }

    /**
     * Creates a default authentication manager that uses the provided
     * configuration directory and user's credentials. Whether the default auth
     * storage is used or not depends on the 'store-auth-creds'</i> option that
     * is looked up in the <i>config</i> file under the <i>[auth]</i> section.
     * Files <i>config</i> and <i>servers</i> will be created (if they still
     * don't exist) in the specified directory (they are the same as those ones
     * you can find in the default SVN's run-time configuration area).
     * 
     * @param configDir
     *            a new location of the run-time configuration area
     * @param userName
     *            a user's name
     * @param password
     *            a user's password
     * @return a default implementation of the credentials and servers
     *         configuration driver interface
     *         
     * @since 1.8.9
     */
    public static ISVNAuthenticationManager createDefaultAuthenticationManager(File configDir, String userName, char[] password) {
        DefaultSVNOptions options = createDefaultOptions(configDir, true);
        boolean store = options.isAuthStorageEnabled();
        return createDefaultAuthenticationManager(configDir, userName, password, store);
    }

    /**
     * @deprecated Use {@link #createDefaultAuthenticationManager(File, String, char[])} method.
     */
    public static ISVNAuthenticationManager createDefaultAuthenticationManager(File configDir, String userName, String password) {
        DefaultSVNOptions options = createDefaultOptions(configDir, true);
        boolean store = options.isAuthStorageEnabled();
        return createDefaultAuthenticationManager(configDir, userName, password, store);
    }

    /**
     * Creates a default authentication manager that uses the provided
     * configuration directory and user's credentials. The
     * <code>storeAuth</code> parameter affects on using the auth storage.
     * 
     * 
     * @param configDir
     *            a new location of the run-time configuration area
     * @param userName
     *            a user's name
     * @param password
     *            a user's password
     * @param storeAuth
     *            if <span class="javakeyword">true</span> then the auth
     *            storage is enabled, otherwise disabled
     * @return a default implementation of the credentials and servers
     *         configuration driver interface
     *         
     * @since 1.8.9
     */
    public static ISVNAuthenticationManager createDefaultAuthenticationManager(File configDir, String userName, char[] password, boolean storeAuth) {
        return createDefaultAuthenticationManager(configDir, userName, password, null, null, storeAuth);
    }

    /**
     * @deprecated Use {@link #createDefaultAuthenticationManager(File, String, char[], boolean)} method.
     */
    public static ISVNAuthenticationManager createDefaultAuthenticationManager(File configDir, String userName, String password, boolean storeAuth) {
        return createDefaultAuthenticationManager(configDir, userName, password, null, null, storeAuth);
    }

    /**
     * Creates a default authentication manager that uses the provided
     * configuration directory and user's credentials. The
     * <code>storeAuth</code> parameter affects on using the auth storage.
     * 
     * 
     * @param configDir
     *            a new location of the run-time configuration area
     * @param userName
     *            a user's name
     * @param password
     *            a user's password
     * @param privateKey
     *            a private key file for SSH session
     * @param passphrase
     *            a passphrase that goes with the key file
     * @param storeAuth
     *            if <span class="javakeyword">true</span> then the auth
     *            storage is enabled, otherwise disabled
     * @return a default implementation of the credentials and servers
     *         configuration driver interface
     *         
     * @since 1.8.9
     */
    public static ISVNAuthenticationManager createDefaultAuthenticationManager(File configDir, String userName, char[] password, File privateKey, char[] passphrase, boolean storeAuth) {
        // check whether we are running inside Eclipse.
        if (isEclipseKeyringSupported()) {
            // use reflection to allow compilation when there is no Eclipse.
            try {
                ClassLoader loader = SVNWCUtil.class.getClassLoader();
                if (loader == null) {
                    loader = ClassLoader.getSystemClassLoader();
                }
                Class<?> managerClass = loader.loadClass(ECLIPSE_AUTH_MANAGER_CLASSNAME);
                if (managerClass != null) {
                    Constructor<?> method = managerClass.getConstructor(new Class[] {
                            File.class, Boolean.TYPE, String.class, char[].class, File.class, char[].class
                    });
                    if (method != null) {
                        return (ISVNAuthenticationManager) method.newInstance(new Object[] {
                                configDir, storeAuth ? Boolean.TRUE : Boolean.FALSE, userName, password, privateKey, passphrase
                        });
                    }
                }
            } catch (Throwable e) {
            }
        }
        return new DefaultSVNAuthenticationManager(configDir, storeAuth, userName, password, privateKey, passphrase);
    }

    /**
     * @deprecated Use {@link #createDefaultAuthenticationManager(File, String, char[], File, char[], boolean)} method.
     */
    public static ISVNAuthenticationManager createDefaultAuthenticationManager(File configDir, String userName, String password, File privateKey, String passphrase, boolean storeAuth) {
        final char[] passwordValue = password != null ? password.toCharArray() : null;
        final char[] passphraseValue = passphrase != null ? passphrase.toCharArray() : null;
        return createDefaultAuthenticationManager(configDir, userName, passwordValue, privateKey, passphraseValue, storeAuth);
    }
    
    /**
     * Creates a default run-time configuration options driver that uses the
     * provided configuration directory.
     * 
     * <p>
     * If <code>dir</code> is not <span class="javakeyword">null</span> then
     * all necessary config files (in particular <i>config</i> and <i>servers</i>)
     * will be created in this directory if they still don't exist. Those files
     * are the same as those ones you can find in the default SVN's run-time
     * configuration area.
     * 
     * @param dir
     *            a new location of the run-time configuration area
     * @param readonly
     *            if <span class="javakeyword">true</span> then run-time
     *            configuration options are available only for reading, if <span
     *            class="javakeyword">false</span> then those options are
     *            available for both reading and writing
     * @return a default implementation of the run-time configuration options
     *         driver interface
     */
    public static DefaultSVNOptions createDefaultOptions(File dir, boolean readonly) {
        return new DefaultSVNOptions(dir, readonly);
    }

    /**
     * Creates a default run-time configuration options driver that uses the
     * default SVN's run-time configuration area.
     * 
     * @param readonly
     *            if <span class="javakeyword">true</span> then run-time
     *            configuration options are available only for reading, if <span
     *            class="javakeyword">false</span> then those options are
     *            available for both reading and writing
     * @return a default implementation of the run-time configuration options
     *         driver interface
     * @see #getDefaultConfigurationDirectory()
     */
    public static DefaultSVNOptions createDefaultOptions(boolean readonly) {
        return new DefaultSVNOptions(null, readonly);
    }

    /**
     * Determines if a directory is under version control.
     * 
     * @param dir
     *            a directory to check
     * @return <span class="javakeyword">true</span> if versioned, otherwise
     *         <span class="javakeyword">false</span>
     */
    public static boolean isVersionedDirectory(File dir) {
        return SvnOperationFactory.isVersionedDirectory(dir);
    }

    /**
     * Determines if a directory is the root of the Working Copy.
     * 
     * @param versionedDir
     *            a versioned directory to check
     * @return <span class="javakeyword">true</span> if
     *         <code>versionedDir</code> is versioned and the WC root (or the
     *         root of externals if <code>considerExternalAsRoot</code> is
     *         <span class="javakeyword">true</span>), otherwise <span
     *         class="javakeyword">false</span>
     * @throws SVNException
     * @since 1.1
     */
    public static boolean isWorkingCopyRoot(final File versionedDir) throws SVNException {
        return SvnOperationFactory.isWorkingCopyRoot(versionedDir);
    }

    /**
     * @param versionedDir
     *            a versioned directory to check
     * @param externalIsRoot
     * @return <span class="javakeyword">true</span> if
     *         <code>versionedDir</code> is versioned and the WC root (or the
     *         root of externals if <code>considerExternalAsRoot</code> is
     *         <span class="javakeyword">true</span>), otherwise <span
     *         class="javakeyword">false</span>
     * @throws SVNException
     * @deprecated use {@link #isWorkingCopyRoot(File)}} instead
     */
    public static boolean isWorkingCopyRoot(final File versionedDir, boolean externalIsRoot) throws SVNException {
        if (isWorkingCopyRoot(versionedDir)) {
            if (!externalIsRoot) {
                return true;

            }
            File root = SvnOperationFactory.getWorkingCopyRoot(versionedDir, false);
            return root.equals(versionedDir);
        }
        return false;
    }

    /**
     * Returns the Working Copy root directory given a versioned directory that
     * belongs to the Working Copy.
     * 
     * <p>
     * If both <span>versionedDir</span> and its parent directory are not
     * versioned this method returns <span class="javakeyword">null</span>.
     * 
     * @param versionedDir
     *            a directory belonging to the WC which root is to be searched
     *            for
     * @param stopOnExternals
     *            if <span class="javakeyword">true</span> then this method
     *            will stop at the directory on which any externals definitions
     *            are set
     * @return the WC root directory (if it is found) or <span
     *         class="javakeyword">null</span>.
     * @throws SVNException
     */
    public static File getWorkingCopyRoot(File versionedDir, boolean stopOnExternals) throws SVNException {
        return SvnOperationFactory.getWorkingCopyRoot(versionedDir, stopOnExternals);
    }
    
    public static synchronized boolean isEclipseKeyringSupported() {
        if (ourIsEclipse == null) {
            ourIsEclipse = Boolean.FALSE;
            try {
                ClassLoader loader = SVNWCUtil.class.getClassLoader();
                if (loader == null) {
                    loader = ClassLoader.getSystemClassLoader();
                }
                final Class<?> platform = loader.loadClass("org.eclipse.core.runtime.Platform");
                final Method isRunning = platform.getMethod("isRunning", new Class[0]);
                final Object result = isRunning.invoke(null, new Object[0]);
                if (result != null && Boolean.TRUE.equals(result)) {
                    final EclipseVersion authBundleVersion = getBundle("org.eclipse.core.runtime.compatibility.auth");
                    boolean supportsKeyring = false;
                    if (authBundleVersion != null && authBundleVersion.major >= 3) {
                        supportsKeyring = true;
                    } else {
                        final EclipseVersion runtimeBundleVersion = getBundle("org.eclipse.core.runtime");
                        supportsKeyring = runtimeBundleVersion.major < 3 || (runtimeBundleVersion.major == 3 && runtimeBundleVersion.minor <= 2);
                    }
                    ourIsEclipse = supportsKeyring ? Boolean.TRUE : Boolean.FALSE;
                }
            } catch (Throwable th) {
            }
        }
        return ourIsEclipse.booleanValue();
    }
    
    private static EclipseVersion getBundle(String bundleName) throws Throwable {
        ClassLoader loader = SVNWCUtil.class.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        final Class<?> platform = loader.loadClass("org.eclipse.core.runtime.Platform");
        if (platform == null) {
            return null;
        }
        final Method getBundle = platform.getMethod("getBundle", new Class[] {String.class});
        final Object bundle = getBundle.invoke(null, new Object[] {bundleName});
        if (bundle == null) {
            return null;
        }
        final Class<?> bundleClazz = loader.loadClass("org.osgi.framework.Bundle");        
        final Method getVersion = bundleClazz.getMethod("getVersion", new Class[0]);
        final Object version = getVersion.invoke(bundle, new Object[0]);
        if (version == null) {
            return null;
        }
        final Class<?> versionClazz = loader.loadClass("org.osgi.framework.Version");
        final Method getMajor = versionClazz.getMethod("getMajor", new Class[0]);
        final Method getMinor = versionClazz.getMethod("getMinor", new Class[0]);
        
        final Object major = getMajor.invoke(version, new Object[0]);
        final Object minor = getMinor.invoke(version, new Object[0]);
        
        final EclipseVersion result = new EclipseVersion();
        result.major = (Integer) major;
        result.minor = (Integer) minor;
        return result;
        
    }
    
    private static class EclipseVersion {
        int major; 
        int minor; 
    }
}