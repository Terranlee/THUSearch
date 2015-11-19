# -*- coding: utf-8 -*-   
import xml.dom.minidom
import os
import os.path

class XMLGenerator(object):
    """docstring for XMLGenerator"""
    def __init__(self, root):
        super(XMLGenerator, self).__init__()
        self.root_dir = root
        self.xml_log = open('xml.log', 'w')
        self.suffix_list = ['.html', '.jpg', '.JPG', '.png', '.PNG', '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx']
        self.file_type = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx']
        self.pic_type = ['.jpg', '.JPG', '.png', '.PNG']
        self.href_to_num = dict()
        self.pr_answer = list()

    def load_pagerank(self):
        content = open('href_to_num').readlines()
        for i in content:
            li = i.split('!!!!')
            href = li[0].strip()
            num = int(li[1].strip())
            self.href_to_num[href] = num
        print 'pagerank lenth: %d' % (len(self.href_to_num))
        for i in range(0, len(self.href_to_num)):
            self.pr_answer.append(0.0)
        value = open('pr_answer').readlines()
        for i in value:
            li = i.split(' ')
            num = int(li[0].strip())
            val = float(li[1].strip())
            self.pr_answer[num] = val

    def __del__(self):
        self.xml_log.close()

    #return type, content path, anchor, pagerank
    def parse_file(self, dir, f):
        filename = dir + os.sep + f
        (prefix, suffix) = os.path.splitext(filename)
        print filename
        if suffix == '.content':
            for suf in self.suffix_list:
                if(os.path.exists(prefix + suf)):
                    content = open(filename).readlines()
                    for it in range(0, len(content)):
                        content[it] = content[it].decode('utf-8').strip()
                    if suf == '.html':
                        if len(content) == 3 and len(content[2]) < 2:
                            content.append(content[0])
                        if len(content) == 2:
                            content.append(content[0])
                        if len(content) != 3:
                            warning = 'html: %s\n' % filename
                            self.xml_log.write(warning)

                        html_num = 0
                        search = (prefix + suf)[1:].replace('\\', '/')
                        if self.href_to_num.has_key(search):
                            html_num = self.href_to_num[search]
                            pgrank = '%.15f' % (self.pr_answer[html_num])
                        else:
                            pgrank = ''
                        return ('.html', filename, content[2].strip(), pgrank)
                    elif suf in self.pic_type:
                        if len(content) != 3:
                            warning = 'pics: %s\n' % filename
                            self.xml_log.write(warning)
                        return (suf, filename, content[2].strip(), '')
                    elif suf in self.file_type:
                        return (suf, filename, content[0].encode('utf-8'), '')
        else:
            return ('none', 'none', '', '')

    def walk_and_add(self):
        impl = xml.dom.minidom.getDOMImplementation()
        dom = impl.createDocument(None, 'pics', None)
        root = dom.documentElement
        cat = dom.createElement('category')
        cat.setAttribute('name', 'sogou')
        root.appendChild(cat)

        cnt = 0
        all_content = os.walk(self.root_dir)
        for i in all_content:
            for j in i[2]:
                self.xml_log.write(j)
                file_type, content_file, anchor, pagerank = self.parse_file(i[0], j)
                if file_type != 'none':
                    node = dom.createElement('pic')
                    node.setAttribute('id', str(cnt))
                    node.setAttribute('type', file_type[1:])
                    content_file = content_file[1:].replace('\\', '/')
                    node.setAttribute('content', content_file)
                    where = content_file.rfind('/')
                    node.setAttribute('anchor', content_file[where+1:])
                    node.setAttribute('pr', pagerank)
                    cnt = cnt + 1
                    cat.appendChild(node)
            if cnt % 1000 == 0:
                print cnt
        output = open('doc.xml', 'w')
        dom.writexml(output, addindent = ' ', newl = '\n', encoding='utf-8')
        output.close()

def main():
    root_dir = './publish/files'
    xg = XMLGenerator(root_dir)
    #xg.load_pagerank()
    xg.walk_and_add()

if __name__ == '__main__':
    main()
