# Minecraft Server File Specification

## What is this?
This is the specification for a File that is supposed to be distributed together or seperated from the modpack.
It is supposed to be used by server launchers (_like this one_) to know what it is supposed to do.

## Why?
You might ask, why not just throw the client files next to a forge installer and then call it a day?
You are correct, you can do this if you set it up on your local server, but that is a lot of manual labor.

But it allows for more:
* Reduced size of the server files when download and uploading to a server.
* As it is not launching the server directly but a subprocess it allows for specifying java args easily, 
    which, might not always be possible on some hosting providers.
* This file format is not bound to any program, modpack, or even programming language!  
    A parser could be written for any other utility program to take care of the special problems specified in the file.      
* With the use of wildcard options and regex selectors it could be made to even work across modpack versions.

## Format
See `server-setup-config.yaml` for a example file how this file should be layouted.