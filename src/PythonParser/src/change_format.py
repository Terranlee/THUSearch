# -*- coding: utf-8 -*-   
import re
import os.path
import os

def test():
	content = open('test.content').readlines()
	for c in content:
		c = c.decode('utf-8')
		print c
	output = open('output.content', 'w')
	for c in content:
		output.write(c)
		output.write('\n')
	output.close()

def formulate_file(root):
	all_data = os.walk(root)
	for i in all_data:
		for j in i[2]:
			suffix = os.path.splitext(j)[1]
			if suffix == '.content':
				print j
				input_file = open(i[0] + os.sep + j)
				content = input_file.readlines()
				input_file.close()
				for it in range(0, len(content)):
					content[it] = content[it].decode('utf-8', 'ignore')
				fff = u'星期四 星期五 星期六'
				where = content[1].find(fff)
				if where >= 0 and where < 150:
					content[1] = content[1][where+len(fff):]
				output = open(i[0] + os.sep + j, 'w')
				for c in content:
					output.write(c.encode('utf-8'))
				output.close()

def main():
	root_dir = 'publish'
	formulate_file(root_dir)
	#test()

if __name__ == '__main__':
	main()