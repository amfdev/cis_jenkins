def call(String name, Map files, Map deps) {
	dir(name)
	{
		files.each()
		{
			dir(it.key)
			{
				unstash(it.value)
			}
		}
		dir("DEBIAN")
		{
			File file = new File("control")
			file.write "Package: " + name + System.getProperty("line.separator")
			file << "Version: 1.0-1" + System.getProperty("line.separator")
			file << "Maintainer: name >mail.gmail.com" + System.getProperty("line.separator")
			file << "Architecture: all" + System.getProperty("line.separator")
			file << "Section: misc" + System.getProperty("line.separator")
			file << "Description: short description" + System.getProperty("line.separator")
			file << " long description line 1. line2" + System.getProperty("line.separator")
			file << "Depends: "
			deps.each()
			{
				file << it.key <<"("<< it.value << "),"
			}
		}
		fakeroot dpkg-deb --build
	}
	bat '''
		wsl fakeroot dpkg-deb --build ${name}
	'''
	bat '''
		mv ${name}.deb ${name}_1.0-1_all.deb
	'''
}
