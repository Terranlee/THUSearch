from HTMLParser import HTMLParser
import string
import os.path
import os
import re

counter = 0

class MLStripper(HTMLParser):
    def __init__(self):
        self.reset()
        self.fed = []
    def handle_data(self, d):
        self.fed.append(d)
    def get_data(self):
        return ''.join(self.fed)
                
class MyHTMLParser(object):
    """docstring for MyHTMLParser"""
    def __init__(self):
        super(MyHTMLParser, self).__init__()
        self.stripper = MLStripper()

        self.remove_pattern = re.compile(r'[:;@`,~%&$#\'\"+-.()<>!=|\\/?*^{}]|\[|\]|\w*')
        self.title_pattern = re.compile(r'<title>([\s\S]*?)</title>')
        self.charset_pattern = re.compile(r'charset[\s\S]*?>')
        self.picture_pattern = re.compile(r'[\'\"]([^\'\"]*?(\.jpg|\.JPG|\.png|\.PNG))[\'\"]')
        self.anchor_pattern = re.compile(r'<a href=[\"\'](.*?)[\"\'].*?>(.*?)</a>', re.S)

        self.root_dir = ''
        self.stop_jpgs = set()
        self.anchor_hash = set()
        content = open('stop_jpgs').readlines()
        for i in content:
            self.stop_jpgs.add(i.strip())
        self.log_file = open('html.log', 'w')

    def __del__(self):
        self.log_file.close()

    def strip_tags(self, html):
        s = MLStripper()
        s.feed(html)
        return s.get_data()

    def chinese_only(self, html):
        html = re.sub(self.remove_pattern, "", html)
        return re.sub("\s+", " ", html)

    def get_anchor(self, html):
        output = open('anchor', 'a')
        ans = re.findall(self.anchor_pattern, html)
        for i in ans:
            http = i[0]
            words = i[1]
            words = re.sub(self.remove_pattern, "", words)
            words = re.sub("\s+", " ", words)
            if http.find('http') == 0 or len(http) < 40 or http.find('?') != -1:
                continue
            if len(words) < 2 or words in self.anchor_hash:
                continue
            self.anchor_hash.add(words)
            output.write(http[1:].encode('utf-8') + '!!!!' + words.encode('utf-8'))
            output.write('\n')
        output.close()

    def get_title(self, html, filename):
        answer = re.search(self.title_pattern, html)
        if answer:
            content_title = answer.group(1)
            # more operations? such as trim or else?
            return content_title
        else:
            where = filename.rfind('/')
            return filename[where+1:]

    def detect_codec(self, html):
        ss = self.charset_pattern.search(html)
        if ss:
            char = ss.group(0).lower()
            index = char.find('gb')
            if index == -1:
                return 'UTF-8'
            else:
                return 'GBK'
        return 'UTF-8'

    def with_picture(self, html):
        all_pics = self.picture_pattern.findall(html)
        real_pics = list()
        for i in all_pics:
            real_url = i[0]
            if (not real_url in self.stop_jpgs) and (real_url.find('http') != 0):
                real_url = real_url.replace('/', os.sep)
                real_pics.append(real_url[1:])
        return real_pics
    
    def write(self, filename, title, content):
        output = open(filename, 'w')
        output.write(title.encode('utf-8'))
        output.write('\n')
        output.write(content.encode('utf-8'))
        output.write('\n')
        output.close()

    def add_content_to_picture(self, pic_list, title, content, origin_file):
        top = "news.tsinghua.edu.cn"
        where = origin_file.find(top)
        origin_file = origin_file[where+3:].replace(os.sep,'/')
        for i in pic_list:
            if os.path.exists(self.root_dir + i):
                filename = self.root_dir + os.path.splitext(i)[0] + '.content'
                output = open(filename, 'w')
                output.write(title.encode('utf-8') + '\n')
                output.write(content.encode('utf-8') + '\n')
                output.write(origin_file.encode('utf-8') + '\n')
                output.close()
            else:
                err_data = ('!!!!! can not find picture ' + i ).encode('utf-8')
                self.log_file.write(err_data)
                self.log_file.write('\n')

    def parse_file(self, filename):
        data_in = open(filename).read()
        codec = self.detect_codec(data_in)
        data_in = data_in.decode(codec, 'ignore')

        all_pictures = self.with_picture(data_in)
        self.get_anchor(data_in)
        title = self.get_title(data_in, filename)

        clear_content = self.strip_tags(data_in)
        content = self.chinese_only(clear_content)

        output_name = self.root_dir + os.path.splitext(filename)[0] + '.content'
        self.write(output_name, title, content)
        if not len(all_pictures) == 0:
            self.add_content_to_picture(all_pictures, title, content, filename)

def main():
    parser = MyHTMLParser()
    root_dir = '.'
    html_suffix = ['.html']

    all_data = os.walk(root_dir)
    cnt = 0
    for i in all_data:
        cnt = cnt + 1
        if cnt % 500 == 0:
            print cnt

        for j in i[2]:
            # j is a filename
            suffix = os.path.splitext(j)[1]
            if suffix in html_suffix:
                parser.parse_file(i[0] + os.sep + j)

if __name__=='__main__':
    main()
