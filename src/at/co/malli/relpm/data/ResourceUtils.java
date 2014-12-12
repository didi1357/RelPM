/* 
 * Copyright owner of this file is Dónal (domurtag@yahoo.co.uk)
 * Source: http://stackoverflow.com/questions/204784/how-to-construct-a-relative-path-in-java-from-two-absolute-paths-or-urls
 * 
 * It was reformatted and extended, with a main function containing the tests written by Dónal, from Didi1357.
 * The Exception Type was also changed by Didi1357.
 */
package at.co.malli.relpm.data;


/**
 * @author Dónal (domurtag@yahoo.co.uk)
 */
import java.io.File;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;


public class ResourceUtils
{

  /**
   * Get the relative path from one file to another, specifying the directory separator. If one of the provided
   * resources does not exist, it is assumed to be a file unless it ends with '/' or '\'.
   *
   * @param targetPath targetPath is calculated to this file
   * @param basePath basePath is calculated from this file
   * @param pathSeparator directory separator. The platform default is not assumed so that we can test Unix behaviour
   * when running on Windows (for example)
   * @return
   */
  public static String getRelativePath (String targetPath, String basePath, String pathSeparator) throws PathResolutionException
  {

    // Normalize the paths
    String normalizedTargetPath = FilenameUtils.normalizeNoEndSeparator(targetPath);
    String normalizedBasePath = FilenameUtils.normalizeNoEndSeparator(basePath);

    // Undo the changes to the separators made by normalization
    if (pathSeparator.equals("/"))
    {
      normalizedTargetPath = FilenameUtils.separatorsToUnix(normalizedTargetPath);
      normalizedBasePath = FilenameUtils.separatorsToUnix(normalizedBasePath);
    }
    else if (pathSeparator.equals("\\"))
    {
      normalizedTargetPath = FilenameUtils.separatorsToWindows(normalizedTargetPath);
      normalizedBasePath = FilenameUtils.separatorsToWindows(normalizedBasePath);
    }
    else
    {
      throw new IllegalArgumentException("Unrecognised dir separator '" + pathSeparator + "'");
    }

    String[] base = normalizedBasePath.split(Pattern.quote(pathSeparator));
    String[] target = normalizedTargetPath.split(Pattern.quote(pathSeparator));

    // First get all the common elements. Store them as a string,
    // and also count how many of them there are.
    StringBuffer common = new StringBuffer();

    int commonIndex = 0;
    while (commonIndex < target.length && commonIndex < base.length && target[commonIndex].equals(base[commonIndex]))
    {
      common.append(target[commonIndex] + pathSeparator);
      commonIndex++;
    }

    if (commonIndex == 0)
    {
      // No single common path element. This most
      // likely indicates differing drive letters, like C: and D:.
      // These paths cannot be relativized.
      throw new PathResolutionException("No common path element found for '" + normalizedTargetPath + "' and '"
                                        + normalizedBasePath + "'");
    }

    // The number of directories we have to backtrack depends on whether the base is a file or a dir
    // For example, the relative path from
    //
    // /foo/bar/baz/gg/ff to /foo/bar/baz
    // 
    // ".." if ff is a file
    // "../.." if ff is a directory
    //
    // The following is a heuristic to figure out if the base refers to a file or dir. It's not perfect, because
    // the resource referred to by this path may not actually exist, but it's the best I can do
    boolean baseIsFile = true;

    File baseResource = new File(normalizedBasePath);

    if (baseResource.exists())
    {
      baseIsFile = baseResource.isFile();
    }
    else if (basePath.endsWith(pathSeparator))
    {
      baseIsFile = false;
    }

    StringBuffer relative = new StringBuffer();

    if (base.length != commonIndex)
    {
      int numDirsUp = baseIsFile ? base.length - commonIndex - 1 : base.length - commonIndex;

      for (int i = 0; i < numDirsUp; i++)
      {
        relative.append(".." + pathSeparator);
      }
    }
    relative.append(normalizedTargetPath.substring(common.length()));
    return relative.toString();
  }


  public static class PathResolutionException extends Exception
  {
    PathResolutionException (String msg)
    {
      super(msg);
    }
  }

  private static void assertEquals(String human, String machine)
  {
    if(human.equals(machine))
      System.out.println(human + " equals " + machine);
    else
      System.out.println(human + " does not equal " + machine + " THE TEST FAILED!!!");
  }

  public static void main (String[] args)
  {
    try
    {
      //testGetRelativePathsUnix()
      assertEquals("stuff/xyz.dat", ResourceUtils.getRelativePath("/var/data/stuff/xyz.dat", "/var/data/", "/"));
      assertEquals("../../b/c", ResourceUtils.getRelativePath("/a/b/c", "/a/x/y/", "/"));
      assertEquals("../../b/c", ResourceUtils.getRelativePath("/m/n/o/a/b/c", "/m/n/o/a/x/y/", "/"));

      String target, base, relPath;

      //testGetRelativePathFileToFile()
      target =  "C:\\Windows\\Boot\\Fonts\\chs_boot.ttf";
      base =  "C:\\Windows\\Speech\\Common\\sapisvr.exe";
      relPath =  ResourceUtils.getRelativePath(target, base, "\\");
      assertEquals("..\\..\\Boot\\Fonts\\chs_boot.ttf", relPath);

      //testGetRelativePathDirectoryToFile()
      target =  "C:\\Windows\\Boot\\Fonts\\chs_boot.ttf";
      base =  "C:\\Windows\\Speech\\Common\\";
      relPath =  ResourceUtils.getRelativePath(target, base, "\\");
      assertEquals("..\\..\\Boot\\Fonts\\chs_boot.ttf", relPath);

      //testGetRelativePathFileToDirectory()
      target =  "C:\\Windows\\Boot\\Fonts";
      base =  "C:\\Windows\\Speech\\Common\\foo.txt";
      relPath =  ResourceUtils.getRelativePath(target, base, "\\");
      assertEquals("..\\..\\Boot\\Fonts", relPath);

      //testGetRelativePathDirectoryToDirectory()
      target =  "C:\\Windows\\Boot\\";
      base =  "C:\\Windows\\Speech\\Common\\";
      String expected = "..\\..\\Boot";
      relPath =  ResourceUtils.getRelativePath(target, base, "\\");
      assertEquals(expected, relPath);

      //testGetRelativePathDifferentDriveLetters()
      target =  "D:\\sources\\recovery\\RecEnv.exe";
      base =  "C:\\Java\\workspace\\AcceptanceTests\\Standard test data\\geo\\";
      ResourceUtils.getRelativePath(target, base, "\\");
    }
    catch (PathResolutionException ex)
    {
      System.out.println(ex.getMessage());
    }
  }
}
