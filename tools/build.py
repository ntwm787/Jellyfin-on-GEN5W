"""Build with the included Gradle wrapper, JDK11 and Android SDK."""
from pathlib import Path
import os,shutil,subprocess,sys
ROOT=Path(__file__).resolve().parents[1];SRC=ROOT/'jellyfin-gen5w'
java=Path(os.environ['JAVA_HOME'])/'bin'/('java.exe' if os.name=='nt' else 'java') if os.environ.get('JAVA_HOME') else shutil.which('java')
if not java:raise SystemExit('Install JDK11 and set JAVA_HOME.')
if not (os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or (SRC/'local.properties').exists()):raise SystemExit('Set ANDROID_HOME or ANDROID_SDK_ROOT.')
tasks=sys.argv[1:] or ['assembleLibreDebug','lintLibreDebug','-Pkotlin.incremental=false']
raise SystemExit(subprocess.call([str(java),'-classpath',str(SRC/'gradle/wrapper/gradle-wrapper.jar'),'org.gradle.wrapper.GradleWrapperMain','--project-dir',str(SRC),'--no-daemon','--console=plain',*tasks],cwd=SRC))
