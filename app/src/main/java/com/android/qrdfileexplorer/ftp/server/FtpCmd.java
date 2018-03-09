/*
Copyright 2009 David Revell

This file is part of SwiFTP.

SwiFTP is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SwiFTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.android.qrdfileexplorer.ftp.server;

import android.util.Log;

import com.android.qrdfileexplorer.ftp.FsSettings;

import java.io.File;
import java.lang.reflect.Constructor;


public abstract class FtpCmd implements Runnable {
    private static final String TAG = FtpCmd.class.getSimpleName();

    protected SessionThread sessionThread;

    protected static CmdMap[] cmdClasses = { new CmdMap("SYST", CmdSYST.class),
            new CmdMap("USER", CmdUSER.class), new CmdMap("PASS", CmdPASS.class),
            new CmdMap("TYPE", CmdTYPE.class), new CmdMap("CWD", CmdCWD.class),
            new CmdMap("PWD", CmdPWD.class), new CmdMap("LIST", CmdLIST.class),
            new CmdMap("PASV", CmdPASV.class), new CmdMap("RETR", CmdRETR.class),
            new CmdMap("NLST", CmdNLST.class), new CmdMap("NOOP", CmdNOOP.class),
            new CmdMap("STOR", CmdSTOR.class), new CmdMap("DELE", CmdDELE.class),
            new CmdMap("RNFR", CmdRNFR.class), new CmdMap("RNTO", CmdRNTO.class),
            new CmdMap("RMD", CmdRMD.class), new CmdMap("MKD", CmdMKD.class),
            new CmdMap("OPTS", CmdOPTS.class), new CmdMap("PORT", CmdPORT.class),
            new CmdMap("QUIT", CmdQUIT.class), new CmdMap("FEAT", CmdFEAT.class),
            new CmdMap("SIZE", CmdSIZE.class), new CmdMap("CDUP", CmdCDUP.class),
            new CmdMap("APPE", CmdAPPE.class), new CmdMap("XCUP", CmdCDUP.class), // synonym
            new CmdMap("XPWD", CmdPWD.class), // synonym
            new CmdMap("XMKD", CmdMKD.class), // synonym
            new CmdMap("XRMD", CmdRMD.class), // synonym
            new CmdMap("MDTM", CmdMDTM.class), //
            new CmdMap("MFMT", CmdMFMT.class), //
            new CmdMap("REST", CmdREST.class), //
            new CmdMap("SITE", CmdSITE.class), //
            new CmdMap("MLST", CmdMLST.class), //
            new CmdMap("MLSD", CmdMLSD.class), //
            new CmdMap("HASH", CmdHASH.class),
            new CmdMap("RANG", CmdRANG.class)
    };

    private static Class<?>[] allowedCmdsWhileAnonymous = { CmdUSER.class, CmdPASS.class, //
            CmdCWD.class, CmdLIST.class, CmdMDTM.class, CmdNLST.class, CmdPASV.class, //
            CmdPWD.class, CmdQUIT.class, CmdRETR.class, CmdSIZE.class, CmdTYPE.class, //
            CmdCDUP.class, CmdNOOP.class, CmdSYST.class, CmdPORT.class, //
            CmdMLST.class, CmdMLSD.class, CmdHASH.class, CmdRANG.class //
    };

    public FtpCmd(SessionThread sessionThread) {
        this.sessionThread = sessionThread;
    }

    @Override
    abstract public void run();

    protected static void dispatchCommand(SessionThread session, String inputString) {
        String[] strings = inputString.split(" ");
        String unrecognizedCmdMsg = "502 Command not recognized\r\n";
        if (strings == null) {
            // There was some egregious sort of parsing error
            String errString = "502 Command parse error\r\n";
            Log.d(TAG, errString);
            session.writeString(errString);
            return;
        }
        if (strings.length < 1) {
            Log.d(TAG, "No strings parsed");
            session.writeString(unrecognizedCmdMsg);
            return;
        }
        String verb = strings[0];
        if (verb.length() < 1) {
            Log.i(TAG, "Invalid command verb");
            session.writeString(unrecognizedCmdMsg);
            return;
        }
        FtpCmd cmdInstance = null;
        verb = verb.trim();
        verb = verb.toUpperCase();
        /*for (int i = 0; i < cmdClasses.length; i++) {

            if (cmdClasses[i].getName().equals(verb)) {
                // We found the correct command. We retrieve the corresponding
                // Class object, get the Constructor object for that Class, and
                // and use that Constructor to instantiate the correct FtpCmd
                // subclass. Yes, I'm serious.
                Constructor<? extends FtpCmd> constructor;
                try {
                    constructor = cmdClasses[i].getCommand().getConstructor(
                            new Class[] { SessionThread.class, String.class });
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "FtpCmd subclass lacks expected " + "constructor ");
                    return;
                }
                try {
                    cmdInstance = constructor.newInstance(new Object[] { session,
                            inputString });
                } catch (Exception e) {
                    Log.e(TAG, "Instance creation error on FtpCmd");
                    return;
                }
            }
        }*/
		 for (int i = 0; i < cmdClasses.length; i++) {

            if (cmdClasses[i].getName().equals(verb)) {
                if (verb.equals("USER")){
                    cmdInstance = new CmdUSER(session,inputString);
                }else if (verb.equals("PASS")){
                    cmdInstance = new CmdPASS(session,inputString);
                }else if (verb.equals("TYPE")){
                    cmdInstance = new CmdTYPE(session,inputString);
                }else if (verb.equals("CWD")){
                    cmdInstance = new CmdCWD(session,inputString);
                }else if (verb.equals("PWD")){
                    cmdInstance = new CmdPWD(session,inputString);
                }else if (verb.equals("LIST")){
                    cmdInstance = new CmdLIST(session,inputString);
                }else if (verb.equals("PASV")){
                    cmdInstance = new CmdPASV(session,inputString);
                }else if (verb.equals("RETR")){
                    cmdInstance = new CmdRETR(session,inputString);
                }else if(verb.equals("NLST")){
                    cmdInstance = new CmdNLST(session,inputString);
                }else if (verb.equals("NOOP")){
                    cmdInstance = new CmdNOOP(session,inputString);
                }else if (verb.equals("STOR")){
                    cmdInstance = new CmdSTOR(session,inputString);
                }else if (verb.equals("DELE")){
                    cmdInstance = new CmdDELE(session,inputString);
                }else if (verb.equals("RNTO")){
                    cmdInstance = new CmdRNTO(session,inputString);
                }else if (verb.equals("RNFR")){
                    cmdInstance = new CmdRNFR(session,inputString);
                }else if (verb.equals("RMD")){
                    cmdInstance = new CmdRMD(session,inputString);
                }else if (verb.equals("MKD")){
                    cmdInstance = new CmdMKD(session,inputString);
                }else if (verb.equals("PORT")){
                    cmdInstance = new CmdPORT(session,inputString);
                }else if (verb.equals("FEAT")){
                    cmdInstance = new CmdFEAT(session,inputString);
                }else if (verb.equals("OPTS")){
                    cmdInstance = new CmdOPTS(session,inputString);
                }else if (verb.equals("QUIT")){
                    cmdInstance = new CmdQUIT(session,inputString);
                }else if (verb.equals("SIZE")){
                    cmdInstance = new CmdSIZE(session,inputString);
                }else if (verb.equals("CDUP")){
                    cmdInstance = new CmdCDUP(session,inputString);
                }else if (verb.equals("XCUP")){
                    cmdInstance = new CmdCDUP(session,inputString);
                }else if (verb.equals("APPE")){
                    cmdInstance = new CmdAPPE(session,inputString);
                }else if (verb.equals("XPWD")){
                    cmdInstance = new CmdPWD(session,inputString);
                }else if (verb.equals("XMKD")){
                    cmdInstance = new CmdMKD(session,inputString);
                }else if (verb.equals("XRMD")){
                    cmdInstance = new CmdRMD(session,inputString);
                }else if (verb.equals("MDTM")){
                    cmdInstance = new CmdMDTM(session,inputString);
                }else if(verb.equals("MFMT")){
                    cmdInstance = new CmdMFMT(session,inputString);
                }else if (verb.equals("REST")){
                    cmdInstance = new CmdREST(session,inputString);
                }else if (verb.equals("SITE")){
                    cmdInstance = new CmdSITE(session,inputString);
                }else if (verb.equals("MLSD")){
                    cmdInstance = new CmdMLSD(session,inputString);
                }else if (verb.equals("HASH")){
                    cmdInstance = new CmdHASH(session,inputString);
                }else if (verb.equals("RANG")){
                    cmdInstance = new CmdRANG(session,inputString);
                }
				
                /*Constructor<? extends FtpCmd> constructor;
				  try {
                    constructor = cmdClasses[i].getCommand().getConstructor(SessionThread.class, String.class);
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "FtpCmd subclass lacks expected " + "constructor ");
                    return;
                }
				try {
                    cmdInstance = constructor.newInstance(new Object[] { session,
                            inputString });
                } catch (Exception e) {
                    Log.e(TAG, "Instance creation error on FtpCmd");
                    return;
                }*/
            }
        }
        if (cmdInstance == null) {
            // If we couldn't find a matching command,
            Log.d(TAG, "Ignoring unrecognized FTP verb: " + verb);
            session.writeString(unrecognizedCmdMsg);
            return;
        }

        if (session.isUserLoggedIn()) {
            cmdInstance.run();
        } else if (session.isAnonymouslyLoggedIn() == true) {
            boolean validCmd = false;
            for (Class<?> cl : allowedCmdsWhileAnonymous) {
                if (cmdInstance.getClass().equals(cl)) {
                    validCmd = true;
                    break;
                }
            }
            if (validCmd == true) {
                cmdInstance.run();
            } else {
                session.writeString("530 Guest user is not allowed to use that command\r\n");
            }
        } else if (cmdInstance.getClass().equals(CmdUSER.class)
                || cmdInstance.getClass().equals(CmdPASS.class)
                || cmdInstance.getClass().equals(CmdQUIT.class)) {
            cmdInstance.run();
        } else {
            session.writeString("530 Login first with USER and PASS, or QUIT\r\n");
        }
    }

    /**
     * An FTP parameter is that part of the input string that occurs after the first
     * space, including any subsequent spaces. Also, we want to chop off the trailing
     * '\r\n', if present.
     *
     * Some parameters shouldn't be logged or output (e.g. passwords), so the caller can
     * use silent==true in that case.
     */
    static public String getParameter(String input, boolean silent) {
        if (input == null) {
            return "";
        }
        int firstSpacePosition = input.indexOf(' ');
        if (firstSpacePosition == -1) {
            return "";
        }
        String retString = input.substring(firstSpacePosition + 1);

        // Remove trailing whitespace
        // todo: trailing whitespace may be significant, just remove \r\n
        retString = retString.replaceAll("\\s+$", "");

        if (!silent) {
            Log.d(TAG, "Parsed argument: " + retString);
        }
        return retString;
    }

    /**
     * A wrapper around getParameter, for when we don't want it to be silent.
     */
    static public String getParameter(String input) {
        return getParameter(input, false);
    }

    public static File inputPathToChrootedFile(File existingPrefix, String param) {
        try {
            if (param.charAt(0) == '/') {
                // The STOR contained an absolute path
                File chroot = FsSettings.getChrootDir();
                return new File(chroot, param);
            }
        } catch (Exception e) {
        }

        // The STOR contained a relative path
        return new File(existingPrefix, param);
    }

    public boolean violatesChroot(File file) {
        try {
            // taking the canonical path as new devices have sdcard symlinked
            // for multiuser support
            File chroot = FsSettings.getChrootDir();
            String canonicalChroot = chroot.getCanonicalPath();
            String canonicalPath = file.getCanonicalPath();
            if (!canonicalPath.startsWith(canonicalChroot)) {
                Log.i(TAG, "Path violated folder restriction, denying");
                Log.d(TAG, "path: " + canonicalPath);
                Log.d(TAG, "chroot: " + chroot.toString());
                return true; // the path must begin with the chroot path
            }
            return false;
        } catch (Exception e) {
            Log.i(TAG, "Path canonicalization problem: " + e.toString());
            Log.i(TAG, "When checking file: " + file.getAbsolutePath());
            return true; // for security, assume violation
        }
    }
}
