"""Package an APK as a USB installer with corresponding source and licenses."""
from pathlib import Path
import argparse,hashlib,io,json,re,zipfile
ROOT=Path(__file__).resolve().parents[1]
def pack(files):
    b=io.BytesIO()
    with zipfile.ZipFile(b,'w',zipfile.ZIP_DEFLATED) as z:
        for name,data in sorted(files.items()):
            i=zipfile.ZipInfo(name,(2026,11,5,0,0,0));i.create_system=3;i.compress_type=zipfile.ZIP_DEFLATED
            i.external_attr=(0o100755 if name.endswith(('.sh','gradlew')) else 0o100644)<<16
            z.writestr(i,data)
    return b.getvalue()
def main():
    p=argparse.ArgumentParser();p.add_argument('--apk',type=Path,required=True);p.add_argument('--out',type=Path,default=ROOT/'dist');a=p.parse_args()
    apk=a.apk.read_bytes();v=json.loads((ROOT/'docs/RELEASE-MANIFEST.json').read_text())['public_version'];name='Jellyfin-on-GEN5W-v'+v
    meta=(ROOT/'packaging/NEMESIS.data').read_bytes()
    for key,limit in [('short-desc',64),('detailed-desc',256)]:
        t=re.search(r'(?m)^'+key+r'=(.*)$',meta.decode());assert t and len(t.group(1).rstrip('\r'))<=limit
    assert ('package-version='+v).encode() in meta
    dep=re.search(r'(?m)^depends=(.*)$',meta.decode());assert dep
    assert set(dep.group(1).strip().strip(chr(34)).split(','))=={'media.codecs@>=2.1','keyboard.hackers@>=2.0.1','keyboard.activate@>=2.0.1'}
    with zipfile.ZipFile(io.BytesIO(apk)) as z:assert z.testzip() is None
    inner={'NEMESIS.data':meta,'app/Jellyfin-on-GEN5W.apk':apk,'scripts/pre.sh':(ROOT/'packaging/scripts/pre.sh').read_bytes()}
    for folder,prefix in [('network-guard','guard'),('licenses','licenses')]:
        for f in (ROOT/folder).iterdir():
            if f.is_file():inner[prefix+'/'+f.name]=f.read_bytes()
    for f in ['README.md','SOURCE-CODE.md','CREDITS.md','THIRD-PARTY-NOTICES.md','MODIFICATIONS.md']:inner[f]=(ROOT/f).read_bytes()
    inner['TESTED.md']=(ROOT/'docs/TESTED.md').read_bytes()
    source={}
    for f in ROOT.rglob('*'):
        if not f.is_file():continue
        rel=f.relative_to(ROOT)
        if any(x in {'.git','.gradle','build','dist','work','__pycache__'} for x in rel.parts):continue
        if f.name=='local.properties' or f.suffix.lower() in {'.apk','.zip','.jks','.keystore','.p12','.pem','.key'}:continue
        source['Jellyfin-on-GEN5W/'+rel.as_posix()]=f.read_bytes()
    a.out.mkdir(parents=True,exist_ok=True)
    store=pack(inner)
    assets={name+'-NEMESIS.zip':store,name+'-USB.zip':pack({'NEMESIS.data':(ROOT/'packaging/USB-NEMESIS.data').read_bytes(),'Packages/jellyfin.gen5w.libre.zip':store}),name+'-Source.zip':pack(source)}
    for n,d in assets.items():(a.out/n).write_bytes(d)
    manifest={n:{'sha256':hashlib.sha256(d).hexdigest(),'bytes':len(d)} for n,d in assets.items()}
    (a.out/'SHA256SUMS.txt').write_text(''.join(i['sha256']+'  '+n+'\n' for n,i in sorted(manifest.items())),encoding='utf-8')
    print(json.dumps(manifest,indent=2))
if __name__=='__main__':main()
