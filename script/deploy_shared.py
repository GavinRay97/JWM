#! /usr/bin/env python3
import argparse, build, clean, common, glob, os, platform, revision, subprocess, sys

def main():
  parser = argparse.ArgumentParser()
  parser.add_argument('--dry-run', action='store_true')
  (args, _) = parser.parse_known_args()

  # Build
  build.build_shared()

  # Update poms
  os.chdir(os.path.dirname(__file__) + '/../shared')
  rev = revision.revision()

  with open('deploy/META-INF/maven/org.jetbrains.jwm/jwm-shared/pom.xml', 'r+') as f:
    pomxml = f.read()
    f.seek(0)
    f.write(pomxml.replace('0.0.0-SNAPSHOT', rev))
    f.truncate()

  with open('deploy/META-INF/maven/org.jetbrains.jwm/jwm-shared/pom.properties', 'r+') as f:
    pomprops = f.read()
    f.seek(0)
    f.write(pomprops.replace('0.0.0-SNAPSHOT', rev))
    f.truncate()

  # jwm-shared-*.jar
  print('Packaging jwm-shared-' + rev + ".jar")
  subprocess.check_call(["jar",
    "--create",
    "--file", "target/jwm-shared-" + rev + ".jar",
    "-C", "target/classes", ".",
    "-C", "deploy", "META-INF"
  ])

  if not args.dry_run:
    print('Deploying jwm-shared-' + rev + ".jar")
    subprocess.check_call([
      common.mvn,
      '--batch-mode',
      '--settings', 'deploy/settings.xml',
      '-Dspace.username=Nikita.Prokopov',
      '-Dspace.password=' + os.getenv('SPACE_TOKEN'),
      'deploy:deploy-file',
      "-Dfile=target/jwm-shared-" + rev + ".jar",
      "-DpomFile=deploy/META-INF/maven/org.jetbrains.jwm/jwm-shared/pom.xml",
      "-DrepositoryId=space-maven",
      "-Durl=" + common.space_jwm,
    ])

  # jwm-shared-*-sources.jar
  lombok = common.deps()[0]
  print('Delomboking sources')
  subprocess.check_call([
    "java",
    "-jar",
    lombok,
    "delombok",
    "java",
    "--classpath",
    common.classpath_separator.join(common.deps()),
    "-d", "target/delomboked/org/jetbrains/jwm"
  ])

  print('Packaging jwm-shared-' + rev + "-sources.jar")
  subprocess.check_call(["jar",
    "--create",
    "--file", "target/jwm-shared-" + rev + "-sources.jar",
    "-C", "target/delomboked", ".",
    "-C", "deploy", "META-INF"
  ])

  if not args.dry_run:
    print('Deploying jwm-shared-' + rev + "-sources.jar")
    subprocess.check_call([
      common.mvn,
      '--batch-mode',
      '--settings', 'deploy/settings.xml',
      '-Dspace.username=Nikita.Prokopov',
      '-Dspace.password=' + os.getenv('SPACE_TOKEN'),
      'deploy:deploy-file',
      "-Dpackaging=java-source",
      "-Dfile=target/jwm-shared-" + rev + "-sources.jar",
      "-DpomFile=deploy/META-INF/maven/org.jetbrains.jwm/jwm-shared/pom.xml",
      "-DrepositoryId=space-maven",
      "-Durl=" + common.space_jwm,
    ])

  # Restore poms
  with open('deploy/META-INF/maven/org.jetbrains.jwm/jwm-shared/pom.xml', 'w') as f:
    f.write(pomxml)

  with open('deploy/META-INF/maven/org.jetbrains.jwm/jwm-shared/pom.properties', 'w') as f:
    f.write(pomprops)

  return 0

if __name__ == '__main__':
  sys.exit(main())