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
package org.tmatesoft.svn.core.internal.wc;

import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSLAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.jna.SVNJNAUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DefaultSVNPersistentAuthenticationProvider implements ISVNAuthenticationProvider, ISVNPersistentAuthenticationProvider {

    // Constants allowed for '[auth] password-stores' configuration option.
    public static final String WINDOWS_CRYPTO_API_PASSWORD_STORAGE = "windows-cryptoapi";
    public static final String MAC_OS_KEYCHAIN_PASSWORD_STORAGE = "keychain";
    public static final String GNOME_KEYRING_PASSWORD_STORAGE = "gnome-keyring";

    // Constants used for 'passtype' attribute of .subversion/auth authentication cache area.
    public static final String SIMPLE_PASSTYPE = "simple";
    public static final String WIN_CRYPT_PASSTYPE = "wincrypt";
    public static final String MAC_OS_KEYCHAIN_PASSTYPE = "keychain";
    public static final String GNOME_KEYRING_PASSTYPE = "gnome-keyring";

    private File myDirectory;
    private String myUserName;
    private IPasswordStorage[] myPasswordStorages;
    private ISVNAuthenticationStorageOptions myAuthOptions;
    private DefaultSVNOptions myDefaultOptions;
    private ISVNHostOptionsProvider myHostOptionsProvider;

    protected DefaultSVNPersistentAuthenticationProvider(File directory, String userName, ISVNAuthenticationStorageOptions authOptions,
                                                         DefaultSVNOptions defaultOptions, ISVNHostOptionsProvider hostOptionsProvider) {
        myDirectory = directory;
        myUserName = userName;
        myAuthOptions = authOptions;
        myDefaultOptions = defaultOptions;
        myHostOptionsProvider = hostOptionsProvider;
        myPasswordStorages = createPasswordStorages(defaultOptions);
    }

    protected IPasswordStorage[] createPasswordStorages(DefaultSVNOptions options) {
        final List<IPasswordStorage> storages = new ArrayList<IPasswordStorage>();
        String[] passwordStorageTypes = options.getPasswordStorageTypes();
        for (int i = 0; i < passwordStorageTypes.length; i++) {
            String passwordStorageType = passwordStorageTypes[i];
            if (WINDOWS_CRYPTO_API_PASSWORD_STORAGE.equals(passwordStorageType) && SVNJNAUtil.isWinCryptEnabled()) {
                storages.add(new WinCryptPasswordStorage());
            }
            if (MAC_OS_KEYCHAIN_PASSWORD_STORAGE.equals(passwordStorageType) && SVNJNAUtil.isMacOsKeychainEnabled()) {
                storages.add(new MacOsKeychainPasswordStorage());
            }
            if (GNOME_KEYRING_PASSWORD_STORAGE.equals(passwordStorageType) && SVNJNAUtil.isGnomeKeyringEnabled()) {
                storages.add(new GnomeKeyringPasswordStorage());
            }
        }
        storages.add(new SimplePasswordStorage());
        return (IPasswordStorage[]) storages.toArray(new IPasswordStorage[storages.size()]);
    }

    private IPasswordStorage getPasswordStorage(String passType) {
        if (passType == null) {
            return null;
        }
        for (int i = 0; i < myPasswordStorages.length; i++) {
            IPasswordStorage passwordStorage = myPasswordStorages[i];
            if (passwordStorage.getPassType().equals(passType)) {
                return passwordStorage;
            }
        }
        return null;
    }

    private SVNPasswordAuthentication readSSLPassphrase(String kind, String realm, boolean storageAllowed, SVNURL url) {
        File dir = new File(myDirectory, kind);
        if (!dir.isDirectory()) {
            return null;
        }
        File[] files = SVNFileListUtil.listFiles(dir);
        @SuppressWarnings("unchecked")
        Map<String, SVNAuthentication> matchedAuths = new SVNHashMap();
        for (int i = 0; files != null && i < files.length; i++) {
            File authFile = files[i];
            if (authFile.isFile()) {
                SVNWCProperties props = new SVNWCProperties(authFile, "");
                try {
                    SVNPasswordAuthentication auth = readSSLPassphrase(realm, props, url);
                    if (auth != null) {
                        matchedAuths.put(auth.getUserName(), auth);
                    }
                } catch (SVNException e) {
                    //
                }
            }
        }
        if (matchedAuths.isEmpty()) {
            return null;
        }
        
        SVNPasswordAuthentication matchedAuth = (SVNPasswordAuthentication) matchedAuths.values().iterator().next();
        if (matchedAuths.containsKey(realm)) {
            matchedAuth = (SVNPasswordAuthentication) matchedAuths.get(realm);
        }
        if (matchedAuth != null) {
            return SVNPasswordAuthentication.newInstance("", matchedAuth.getPasswordValue(), storageAllowed, url, false);
        }
        return null;
    }

    private SVNPasswordAuthentication readSSLPassphrase(String expectedCertificatePath, SVNWCProperties props, SVNURL url) throws SVNException {
        SVNProperties values = props.asMap();
        try {
            String storedRealm = values.getStringValue("svn:realmstring");
            if (storedRealm == null || !SVNSSLAuthentication.isCertificatePath(storedRealm)) {
                return null;
            }
            File expectedPath = new File(expectedCertificatePath.replace(File.separatorChar, '/')).getAbsoluteFile();
            File storedPath = new File(storedRealm.replace(File.separatorChar, '/')).getAbsoluteFile();        
            if (!expectedPath.equals(storedPath)) {
                return null;
            }
            String passType = SVNPropertyValue.getPropertyAsString(values.getSVNPropertyValue("passtype"));
            IPasswordStorage passwordStorage = getPasswordStorage(passType);
            if (passType != null && passwordStorage == null) {
                return null;
            }
            char[] passphrase;
            if (passwordStorage != null) {
                passphrase = passwordStorage.readPassphrase(storedRealm, values);
            } else {
                passphrase = SVNPropertyValue.getPropertyAsChars(values.getSVNPropertyValue("passphrase"));
            }
            return SVNPasswordAuthentication.newInstance(storedRealm, passphrase, false, url, false);
        } finally {
            if (values != null) {
                values.clear();
            }
        }
    }
    
    private char[] readPassword(String realm, String userName, IPasswordStorage passwordStorage, SVNProperties authValues) throws SVNException {
        if (passwordStorage != null) {
            return passwordStorage.readPassword(realm, userName, authValues);
        }
        return SVNPropertyValue.getPropertyAsChars(authValues.getSVNPropertyValue("password"));
    }

    private char[] readPassphrase(String realm, IPasswordStorage passwordStorage, SVNProperties authValues) throws SVNException {
        if (passwordStorage != null) {
            return passwordStorage.readPassphrase(realm, authValues);
        }
        return SVNPropertyValue.getPropertyAsChars(authValues.getSVNPropertyValue("passphrase"));
    }

    public SVNAuthentication requestClientAuthentication(String kind, SVNURL url, String realm, SVNErrorMessage errorMessage,
                                                         SVNAuthentication previousAuth, boolean authMayBeStored) {
        realm = preprocessRealm(realm);

        if (ISVNAuthenticationManager.SSL.equals(kind)) {
            if (SVNSSLAuthentication.isCertificatePath(realm)) {
                return readSSLPassphrase(kind, realm, authMayBeStored, url);
            }
            final ISVNHostOptions hostOptions = myHostOptionsProvider.getHostOptions(url);
            String sslClientCert = hostOptions.getSSLClientCertFile(); // PKCS#12
            if (sslClientCert != null && !"".equals(sslClientCert)) {
                if (isMSCapi(sslClientCert)) {
                    String alias = null;
                    if (sslClientCert.lastIndexOf(';') > 0) {
                        alias = sslClientCert.substring(sslClientCert.lastIndexOf(';') + 1);
                    }
                    return SVNSSLAuthentication.newInstance(SVNSSLAuthentication.MSCAPI, alias, authMayBeStored, url, false);
                }

                String sslClientCertPassword = hostOptions.getSSLClientCertPassword();
                File clientCertFile = sslClientCert != null ? new File(sslClientCert) : null;
                final char[] passwordValue = sslClientCertPassword != null ? sslClientCertPassword.toCharArray() : null;
                SVNSSLAuthentication sslAuth = SVNSSLAuthentication.newInstance(clientCertFile, passwordValue, authMayBeStored, url, false);
                if (sslClientCertPassword == null || "".equals(sslClientCertPassword)) {
                    // read from cache at once.
                    final SVNPasswordAuthentication passphrase = readSSLPassphrase(kind, sslClientCert, authMayBeStored, url);
                    if (passphrase != null && passphrase.getPasswordValue() != null) {
                        sslAuth = SVNSSLAuthentication.newInstance(clientCertFile, passphrase.getPasswordValue(), authMayBeStored, url, false);
                    }
                }
                sslAuth.setCertificatePath(sslClientCert);
                return sslAuth;
            }
        }

        File dir = new File(myDirectory, kind);
        if (!dir.isDirectory()) {
            return null;
        }
        String fileName = getAuthFileName(realm);
        File authFile = new File(dir, fileName);
        if (authFile.exists()) {
            SVNWCProperties props = new SVNWCProperties(authFile, "");
            SVNProperties values = null;
            try {
                values = props.asMap();
                String storedRealm = values.getStringValue("svn:realmstring");
                String passType = SVNPropertyValue.getPropertyAsString(values.getSVNPropertyValue("passtype"));
                IPasswordStorage passwordStorage = getPasswordStorage(passType);
                if (passType != null && passwordStorage == null) {
                    return null;
                }
                if (storedRealm == null || !storedRealm.equals(realm)) {
                    return null;
                }

                String userName = SVNPropertyValue.getPropertyAsString(values.getSVNPropertyValue("username"));

                if (!ISVNAuthenticationManager.SSL.equals(kind)) {
                    if (userName == null || "".equals(userName.trim())) {
                        return null;
                    }
                    if (myUserName != null && !myUserName.equals(userName)) {
                        return null;
                    }
                }

                String path = SVNPropertyValue.getPropertyAsString(values.getSVNPropertyValue("key"));
                String port = SVNPropertyValue.getPropertyAsString(values.getSVNPropertyValue("port"));
                port = port == null ? ("" + myDefaultOptions.getDefaultSSHPortNumber()) : port;
                String sslKind = SVNPropertyValue.getPropertyAsString(values.getSVNPropertyValue("ssl-kind"));

                if (ISVNAuthenticationManager.PASSWORD.equals(kind)) {
                    char[] password = readPassword(realm, userName, passwordStorage, values);
                    return SVNPasswordAuthentication.newInstance(userName, password, authMayBeStored, url, password == null);

                } else if (ISVNAuthenticationManager.SSH.equals(kind)) {
                    // get port from config file or system property?
                    int portNumber;
                    try {
                        portNumber = Integer.parseInt(port);
                    } catch (NumberFormatException nfe) {
                        portNumber = myDefaultOptions.getDefaultSSHPortNumber();
                    }
                    if (path != null) {
                        final char[] passphrase = readPassphrase(storedRealm, passwordStorage, values);
                        return SVNSSHAuthentication.newInstance(userName, new File(path), passphrase, portNumber, authMayBeStored, url, false);
                    }
                    final char[] password = readPassword(realm, userName, passwordStorage, values);
                    if (password != null) {
                        return SVNSSHAuthentication.newInstance(userName, password, portNumber, authMayBeStored, url, false);
                    }
                } else if (ISVNAuthenticationManager.USERNAME.equals(kind)) {
                    return SVNUserNameAuthentication.newInstance(userName, authMayBeStored, url, false);
                } else if (ISVNAuthenticationManager.SSL.equals(kind)) {
                    if (isMSCapi(sslKind)) {                        
                        final String alias = SVNPropertyValue.getPropertyAsString(values.getSVNPropertyValue("alias"));
                        return SVNSSLAuthentication.newInstance(SVNSSLAuthentication.MSCAPI, alias, authMayBeStored, url, false);
                    }
                    final char[] passphrase = readPassphrase(storedRealm, passwordStorage, values);
                    SVNSSLAuthentication sslAuth = SVNSSLAuthentication.newInstance(new File(path), passphrase, authMayBeStored, url, false);
                    if (passphrase == null || "".equals(passphrase)) {
                        SVNPasswordAuthentication passphraseAuth = readSSLPassphrase(kind, path, authMayBeStored, url);
                        if (passphraseAuth != null && passphraseAuth.getPasswordValue() != null) {
                            sslAuth = SVNSSLAuthentication.newInstance(new File(path), passphraseAuth.getPasswordValue(), authMayBeStored, url, false);
                        }
                    }
                    sslAuth.setCertificatePath(path);
                    return sslAuth;
                }
            } catch (SVNException e) {
                //
            } finally {
                if (values != null) {
                    values.dispose();
                }
            }
        }
        return null;
    }

    protected String preprocessRealm(String realm) {
        return realm;
    }

    public boolean isMSCapi(String filepath) {
        if (filepath != null && filepath.startsWith(SVNSSLAuthentication.MSCAPI)) {
            return true;
        }
        return false;
    }

    public void saveAuthentication(SVNAuthentication auth, String kind, String realm) throws SVNException {
        File dir = new File(myDirectory, kind);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.isDirectory()) {
            return;
        }
        if (!ISVNAuthenticationManager.SSL.equals(kind) && ("".equals(auth.getUserName()) || auth.getUserName() == null)) {
            return;
        }

        SVNProperties values = new SVNProperties();
        values.put("svn:realmstring", realm);

        if (ISVNAuthenticationManager.PASSWORD.equals(kind)) {
            savePasswordCredential(values, auth, realm);
        } else if (ISVNAuthenticationManager.SSH.equals(kind)) {
            saveSSHCredential(values, auth, realm);
        } else if (ISVNAuthenticationManager.SSL.equals(kind)) {
            if (!saveSSLCredential(values, auth, realm)) {
                return;
            }
        } else if (ISVNAuthenticationManager.USERNAME.equals(kind)) {
            saveUserNameCredential(values, auth);
        }
        // get file name for auth and store password.
        String fileName = getAuthFileName(realm);
        File authFile = new File(dir, fileName);

        if (authFile.isFile()) {
            SVNWCProperties props = new SVNWCProperties(authFile, "");
            try {
                if (!shouldSaveCredentials(kind, values, props.asMap())) {
                    return;
                }
            } catch (SVNException e) {
                //
            }
        }
        File tmpFile = SVNFileUtil.createUniqueFile(dir, "auth", ".tmp", true);
        try {
            SVNWCProperties.setProperties(values, authFile, tmpFile, SVNWCProperties.SVN_HASH_TERMINATOR);
        } finally {
            SVNFileUtil.deleteFile(tmpFile);
        }
    }

    protected String getAuthFileName(String realm) {
        return SVNFileUtil.computeChecksum(realm);
    }

    public int acceptServerAuthentication(SVNURL url, String r, Object serverAuth, boolean resultMayBeStored) {
        return ACCEPTED;
    }

    private boolean shouldSaveCredentials(String kind, SVNProperties newValues, SVNProperties oldValues) throws SVNException {
        assert newValues != null;
        assert oldValues != null;

        if (!ISVNAuthenticationManager.PASSWORD.equals(kind)) {
            return !newValues.equals(oldValues);
        }
        String newUsername = SVNPropertyValue.getPropertyAsString(newValues.getSVNPropertyValue("username"));
        String newPassType = SVNPropertyValue.getPropertyAsString(newValues.getSVNPropertyValue("passtype"));
        String newRealm = SVNPropertyValue.getPropertyAsString(newValues.getSVNPropertyValue("svn:realmstring"));
        IPasswordStorage newPasswordStorage = getPasswordStorage(newPassType);
        char[] newPassword = newPasswordStorage == null ? null : newPasswordStorage.readPassword(newRealm, newUsername, newValues);

        String oldUsername = SVNPropertyValue.getPropertyAsString(oldValues.getSVNPropertyValue("username"));
        String oldPassType = SVNPropertyValue.getPropertyAsString(oldValues.getSVNPropertyValue("passtype"));
        String oldRealm = SVNPropertyValue.getPropertyAsString(oldValues.getSVNPropertyValue("svn:realmstring"));
        IPasswordStorage oldPasswordStorage = getPasswordStorage(oldPassType);
        char[] oldPassword = oldPasswordStorage == null ? null : oldPasswordStorage.readPassword(oldRealm, oldUsername, oldValues);

        if (newUsername != null) {
            if (oldUsername == null) {
                return true;
            } else if (!newUsername.equals(oldUsername)) {
                return true;
            }
        }

        if (newPassword != null) {
            if (oldPassword == null) {
                return true;
            } else if (!newPassword.equals(oldPassword)) {
                return true;
            }
        }

        return false;
    }

    private void saveUserNameCredential(SVNProperties values, SVNAuthentication auth) {
        values.put("username", auth.getUserName());
    }

    private void savePasswordCredential(SVNProperties values, SVNAuthentication auth, String realm) throws SVNException {
        final String userName = auth.getUserName();
        values.put("username", userName);

        boolean storePasswords = myHostOptionsProvider.getHostOptions(auth.getURL()).isStorePasswords();

        if (storePasswords) {
            SVNPasswordAuthentication passwordAuth = (SVNPasswordAuthentication) auth;

            for (int i = 0; i < myPasswordStorages.length; i++) {
                IPasswordStorage passwordStorage = myPasswordStorages[i];
                boolean saved = passwordStorage.savePassword(realm, passwordAuth.getPasswordValue(), passwordAuth, values);
                if (saved) {
                    values.put("passtype", passwordStorage.getPassType());
                    break;
                }
            }
        }
    }

    private void saveSSHCredential(SVNProperties values, SVNAuthentication auth, String realm) throws SVNException {
        values.put("username", auth.getUserName());

        SVNSSHAuthentication sshAuth = (SVNSSHAuthentication) auth;
        boolean storePasswords = myHostOptionsProvider.getHostOptions(auth.getURL()).isStorePasswords();

        IPasswordStorage storage = null;
        if (storePasswords) {
            for (int i = 0; i < myPasswordStorages.length; i++) {
                IPasswordStorage passwordStorage = myPasswordStorages[i];
                final char[] password = sshAuth.getPasswordValue();
                
                boolean saved = passwordStorage.savePassword(realm, password, auth, values);
                if (saved) {
                    values.put("passtype", passwordStorage.getPassType());
                    storage = passwordStorage;
                    break;
                }
            }
        }

        int port = sshAuth.getPortNumber();
        if (sshAuth.getPortNumber() < 0) {
            port = myDefaultOptions.getDefaultSSHPortNumber();
        }
        values.put("port", Integer.toString(port));

        if (sshAuth.getPrivateKeyFile() != null) {
            String path = sshAuth.getPrivateKeyFile().getAbsolutePath();
            if (storage != null) {
                // Pass 'force == true' not to ask user for plain text storage.
                storage.savePassphrase(realm, sshAuth.getPassphraseValue(), sshAuth, values, true);
            } else {
                for (int i = 0; i < myPasswordStorages.length; i++) {
                    IPasswordStorage passwordStorage = myPasswordStorages[i];
                    boolean saved = passwordStorage.savePassphrase(realm, sshAuth.getPassphraseValue(), sshAuth, values, false);
                    if (saved) {
                        values.put("passtype", passwordStorage.getPassType());
                        break;
                    }
                }
            }
            values.put("key", path);
        }
    }

    private boolean saveSSLCredential(SVNProperties values, SVNAuthentication auth, String realm) throws SVNException {
        boolean storePassphrases = myHostOptionsProvider.getHostOptions(auth.getURL()).isStoreSSLClientCertificatePassphrases();
        boolean modified = false;
        
        final char[] passphrase;
        if (auth instanceof SVNPasswordAuthentication) {
            passphrase = ((SVNPasswordAuthentication) auth).getPasswordValue();
        } else {
            if (myAuthOptions.isSSLPassphrasePromptSupported()) {
                // do not save passphrase, it have to be saved already.
                passphrase = null;
            } else if (auth instanceof SVNSSLAuthentication) {
                // otherwise we're in the old-school mode and will save passpharse for host realm,
                // as we used to do before.
                passphrase = ((SVNSSLAuthentication) auth).getPasswordValue();
            } else {
                passphrase = null;
            }
        }
        if (storePassphrases && passphrase != null) {

            for (int i = 0; i < myPasswordStorages.length; i++) {
                IPasswordStorage passwordStorage = myPasswordStorages[i];
                boolean saved = passwordStorage.savePassphrase(realm, passphrase, auth, values, false);
                if (saved) {
                    values.put("passtype", passwordStorage.getPassType());
                    modified = true;
                    break;
                }
            }
        }
        
        if (auth instanceof SVNSSLAuthentication) {
            SVNSSLAuthentication sslAuth = (SVNSSLAuthentication) auth;
            if (SVNSSLAuthentication.SSL.equals(sslAuth.getSSLKind())) {
                if (sslAuth.getCertificateFile() != null) {
                    String path = sslAuth.getCertificatePath();
                    values.put("key", path);
                    modified = true;
                }
            } else if (SVNSSLAuthentication.MSCAPI.equals(sslAuth.getSSLKind())) {
                values.put("ssl-kind", sslAuth.getSSLKind());
                if (sslAuth.getAlias() != null) {
                    values.put("alias", sslAuth.getAlias());
                }
                modified = true;
            }
        }
        return modified;
    }

    public byte[] loadFingerprints(String realm) {
        File dir = new File(myDirectory, "svn.ssh.server");
        if (!dir.isDirectory()) {
            return null;
        }
        File file = new File(dir, getAuthFileName(realm));
        if (!file.isFile()) {
            return null;
        }
        SVNWCProperties props = new SVNWCProperties(file, "");
        SVNProperties values;
        try {
            values = props.asMap();
            String storedRealm = values.getStringValue("svn:realmstring");
            if (!realm.equals(storedRealm)) {
                return null;
            }
            return values.getBinaryValue("hostkey");
        } catch (SVNException e) {
            return null;
        }
    }

    public void saveFingerprints(String realm, byte[] fingerprints) {
        File dir = new File(myDirectory, "svn.ssh.server");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        File file = new File(dir, getAuthFileName(realm));

        SVNProperties values = new SVNProperties();
        values.put("svn:realmstring", realm);
        values.put("hostkey", fingerprints);
        try {
            SVNWCProperties.setProperties(values, file, null, SVNWCProperties.SVN_HASH_TERMINATOR);
        } catch (SVNException e) {
        }
    }

    public interface IPasswordStorage {

        String getPassType();

        boolean savePassword(String realm, char[] password, SVNAuthentication auth, SVNProperties authParameters) throws SVNException;

        char[] readPassword(String realm, String userName, SVNProperties authParameters) throws SVNException;

        boolean savePassphrase(String realm, char[] passphrase, SVNAuthentication auth, SVNProperties authParameters, boolean force) throws SVNException;

        char[] readPassphrase(String realm, SVNProperties authParameters) throws SVNException;
    }

    protected class SimplePasswordStorage implements IPasswordStorage {

        public String getPassType() {
            return SIMPLE_PASSTYPE;
        }

        public boolean savePassword(String realm, char[] password, SVNAuthentication auth, SVNProperties authParameters) throws SVNException {
            if (password == null || auth == null) {
                return false;
            }
            ISVNHostOptions opts = myHostOptionsProvider.getHostOptions(auth == null ?  null : auth.getURL());
            if (opts.isStorePlainTextPasswords(realm, auth)) {
                authParameters.put("password", password, "UTF-8");
                return true;
            }
            return false;
        }

        public char[] readPassword(String realm, String userName, SVNProperties authParameters) {
            final SVNPropertyValue value = authParameters.getSVNPropertyValue("password");
            return SVNPropertyValue.getPropertyAsChars(value);
        }

        public boolean savePassphrase(String realm, char[] passphrase, SVNAuthentication auth, SVNProperties authParameters, boolean force) throws SVNException {
            if (passphrase == null || auth == null) {
                return false;
            }
            ISVNHostOptions opts = myHostOptionsProvider.getHostOptions(auth == null ?  null : auth.getURL());
            if (force || opts.isStorePlainTextPassphrases(realm, auth)) {
                authParameters.put("passphrase", SVNPropertyValue.create(passphrase, "UTF-8"));
                return true;
            }
            return false;
        }

        public char[] readPassphrase(String realm, SVNProperties authParameters) {
            return SVNPropertyValue.getPropertyAsChars(authParameters.getSVNPropertyValue("passphrase"));
        }
    }

    protected class WinCryptPasswordStorage implements IPasswordStorage {

        public String getPassType() {
            return WIN_CRYPT_PASSTYPE;
        }

        public boolean savePassword(String realm, char[] password, SVNAuthentication auth, SVNProperties authParameters) {
            if (password == null) {
                return false;
            }
            char[] encrypted = SVNJNAUtil.encrypt(password);
            if (encrypted == null) {
                return false;
            }
            authParameters.put("password", SVNPropertyValue.create(encrypted, "UTF-8"));
            return true;
        }

        public char[] readPassword(String realm, String userName, SVNProperties authParameters) {
            final char[] encrypted = SVNPropertyValue.getPropertyAsChars(authParameters.getSVNPropertyValue("password"));
            return SVNJNAUtil.decrypt(encrypted);
        }

        public boolean savePassphrase(String realm, char[] passphrase, SVNAuthentication auth, SVNProperties authParameters, boolean force) {
            if (passphrase == null) {
                return false;
            }
            char[] encrypted = SVNJNAUtil.encrypt(passphrase);
            if (encrypted == null) {
                return false;
            }
            authParameters.put("passphrase", SVNPropertyValue.create(encrypted, "UTF-8"));
            return true;
        }

        public char[] readPassphrase(String realm, SVNProperties authParameters) {
            final char[] encrypted = SVNPropertyValue.getPropertyAsChars(authParameters.getSVNPropertyValue("passphrase"));
            return SVNJNAUtil.decrypt(encrypted);
        }
    }

    protected class MacOsKeychainPasswordStorage implements IPasswordStorage {

        public String getPassType() {
            return MAC_OS_KEYCHAIN_PASSTYPE;
        }

        public boolean savePassword(String realm, char[] password, SVNAuthentication auth, SVNProperties authParameters) throws SVNException {
            if (password == null) {
                return false;
            }
            return SVNJNAUtil.addPasswordToMacOsKeychain(realm, auth.getUserName(), password, myAuthOptions.isNonInteractive());
        }

        public char[] readPassword(String realm, String userName, SVNProperties authParameters) throws SVNException {
            return SVNJNAUtil.getPasswordFromMacOsKeychain(realm, userName, myAuthOptions.isNonInteractive());
        }

        public boolean savePassphrase(String realm, char[] passphrase, SVNAuthentication auth, SVNProperties authParameters, boolean force) throws SVNException {
            if (passphrase == null) {
                return false;
            }
            return SVNJNAUtil.addPasswordToMacOsKeychain(realm, null, passphrase, myAuthOptions.isNonInteractive());
        }

        public char[] readPassphrase(String realm, SVNProperties authParameters) throws SVNException {
            return SVNJNAUtil.getPasswordFromMacOsKeychain(realm, null, myAuthOptions.isNonInteractive());
        }
    }

    protected class GnomeKeyringPasswordStorage implements IPasswordStorage {

        public String getPassType() {
            return GNOME_KEYRING_PASSTYPE;
        }

        public boolean savePassword(String realm, char[] password, SVNAuthentication auth, SVNProperties authParameters) throws SVNException {
            if (password == null) {
                return false;
            }
            boolean nonInteractive = myAuthOptions.isNonInteractive();
            ISVNGnomeKeyringPasswordProvider keyringPasswordProvider = myAuthOptions.getGnomeKeyringPasswordProvider();
            return SVNJNAUtil.addPasswordToGnomeKeyring(realm, auth.getUserName(), password, nonInteractive, keyringPasswordProvider);
        }

        public char[] readPassword(String realm, String userName, SVNProperties authParameters) throws SVNException {
            boolean nonInteractive = myAuthOptions.isNonInteractive();
            ISVNGnomeKeyringPasswordProvider keyringPasswordProvider = myAuthOptions.getGnomeKeyringPasswordProvider();
            return SVNJNAUtil.getPasswordFromGnomeKeyring(realm, userName, nonInteractive, keyringPasswordProvider);
        }

        public boolean savePassphrase(String realm, char[] passphrase, SVNAuthentication auth, SVNProperties authParameters, boolean force) throws SVNException {
            if (passphrase == null) {
                return false;
            }
            boolean nonInteractive = myAuthOptions.isNonInteractive();
            ISVNGnomeKeyringPasswordProvider keyringPasswordProvider = myAuthOptions.getGnomeKeyringPasswordProvider();
            return SVNJNAUtil.addPasswordToGnomeKeyring(realm, null, passphrase, nonInteractive, keyringPasswordProvider);
        }

        public char[] readPassphrase(String realm, SVNProperties authParameters) throws SVNException {
            boolean nonInteractive = myAuthOptions.isNonInteractive();
            ISVNGnomeKeyringPasswordProvider keyringPasswordProvider = myAuthOptions.getGnomeKeyringPasswordProvider();
            return SVNJNAUtil.getPasswordFromGnomeKeyring(realm, null, nonInteractive, keyringPasswordProvider);
        }
    }
}
