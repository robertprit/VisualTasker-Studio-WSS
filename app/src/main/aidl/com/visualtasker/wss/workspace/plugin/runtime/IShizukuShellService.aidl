package com.visualtasker.wss.workspace.plugin.runtime;

interface IShizukuShellService {
    String execute(String commandLine, long timeoutMs);
    void destroy();
}
