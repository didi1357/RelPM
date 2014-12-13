[Setup]
AppName=RelPM
AppVersion=1.0
AppPublisher=Dietmar Malli
AppPublisherURL=http://github.com/didi1357/RelPM
AppSupportURL=http://github.com/didi1357/RelPM
DefaultDirName={pf}\RelPM
DefaultGroupName=RelPM
UninstallDisplayIcon={app}\RelPM.ico
Compression=lzma2
SolidCompression=yes
OutputDir=output

[Files]
Source: "./RelPM-packed.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "./RelPM.ico"; DestDir: "{app}"

[Icons]
Name: "{group}\RelPM"; Filename: "{app}\RelPM-packed.jar"; IconFilename: "{app}\RelPM.ico"
Name: "{commondesktop}\RelPM"; Filename: "{app}\RelPM-packed.jar"; IconFilename: "{app}\RelPM.ico"

[Code]
//Source: http://stackoverflow.com/questions/1297773/check-java-is-present-before-installing
function InitializeSetup(): Boolean;
var
 ErrorCode: Integer;
 JavaInstalled : Boolean;
 Result1 : Boolean;
 Versions: TArrayOfString;
 I: Integer;
begin
 if RegGetSubkeyNames(HKLM, 'SOFTWARE\JavaSoft\Java Runtime Environment', Versions) then
 begin
  for I := 0 to GetArrayLength(Versions)-1 do
   if JavaInstalled = true then
   begin
    //do nothing
   end else
   begin
    if ( Versions[I][2]='.' ) and ( ( StrToInt(Versions[I][1]) > 1 ) or ( ( StrToInt(Versions[I][1]) = 1 ) and ( StrToInt(Versions[I][3]) >= 6 ) ) ) then
    begin
     JavaInstalled := true;
    end else
    begin
     JavaInstalled := false;
    end;
   end;
 end else
 begin
  JavaInstalled := false;
 end;


 //JavaInstalled := RegKeyExists(HKLM,'SOFTWARE\JavaSoft\Java Runtime Environment\1.9');
 if JavaInstalled then
 begin
  Result := true;
 end else
    begin
  Result1 := MsgBox('This tool requires Java Runtime Environment version 1.6 or newer to run. Please download and install the JRE and run this setup again. Do you want to download it now?',
   mbConfirmation, MB_YESNO) = idYes;
  if Result1 = false then
  begin
   Result:=false;
  end else
  begin
   Result:=false;
   ShellExec('open',
    'http://www.java.com/getjava/',
    '','',SW_SHOWNORMAL,ewNoWait,ErrorCode);
  end;
    end;
end;


end.
