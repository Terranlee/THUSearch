# -*- coding: utf-8 -*-
from __future__ import division

import re
import os.path
import os

class PRcalc(object):
    """docstring for PRcalc"""
    def __init__(self):
        super(PRcalc, self).__init__()
        self.graph = dict()
        self.num2name = dict()
        self.pagerank_log = open('pagerank.log', 'w')

    def __del__(self):
        self.pagerank_log.close()

    def load(self):
        content = open('graph').readlines()
        for i in content:
            li = i.strip().split(':')
            vertex = int(li[0])
            edges = li[1:]
            for i in range(0, len(edges)-1):
                edges[i] = int(edges[i])
            self.graph[vertex] = edges[:len(edges)-1]

        content = open('href_to_num').readlines()
        for i in content:
            li = i.strip().split('!!!!')
            num = int(li[1])
            name = li[0]
            self.num2name[num] = name
        print 'parse pagerank done'

    def calc(self):
        alpha = 0.2
        TN = 20
        S = 0.0

        PR = dict()
        I =  dict()
        graphlist = list(self.graph.keys())
        nodelist = list(self.num2name.keys())
        N = len(nodelist)

        for i in nodelist:
            PR[i] = 1/N
            I[i] = alpha/N
            if self.graph.has_key(i) == False or len(self.graph[i]) == 0:
                S = S + PR[i]

        for k in range(0, TN):
            print 'iteration %d' % (k)
            for i in graphlist:
                for j in self.graph[i]:
                    I[j] = I[j] + (1 - alpha) * PR[i]/len(self.graph[i])

            for i in nodelist:
                PR[i] = I[i] + (1 - alpha) * S/N
                I[i] = alpha/N

            S = 0.0
            for i in nodelist:
                if self.graph.has_key(i) == False or len(self.graph[i]) == 0:
                    S = S + PR[i]
        print 'iteration end'

        data = []
        for i in PR:
            data.append((PR[i], i))
        data.sort()
        output = open('pr_answer', 'w')
        for i in data:
            content = "%d %.15f\n" %(i[1],i[0])
            output.write(content)
        output.close()
        
    def run(self):
        self.load()
        self.calc()
        
class PRprocess(object):
    """docstring for PRprocess"""
    def __init__(self):
        super(PRprocess, self).__init__()
        self.html_pattern = re.compile(r'<a href=[\"\']([^\"\']*?\.html)[\"\']', re.S)
        self.href_to_num = dict()
        self.out_link_network = dict()
        self.counter = 0
        self.pagerank_get = open('graph.log', 'w')

    def __del__(self):
        self.pagerank_get.close()

    def add_to_network(self, page, link):
        if page in self.out_link_network:
            self.out_link_network[page].add(link)
        else:
            self.out_link_network[page] = set()
            self.out_link_network[page].add(link)

    def add_to_dict(self, filename):
        if not filename in self.href_to_num:
            self.href_to_num[filename] = self.counter
            self.counter = self.counter + 1

    def parse_file(self, filename):
        content = open(filename).read()
        real_url = filename[1:].replace('\\', '/')
        self.add_to_dict(real_url)
        current = self.href_to_num[real_url]
        ans = re.findall(self.html_pattern, content)

        for i in ans:
            if i.find('http') == 0:
                continue
            i = i.strip()
            self.add_to_dict(i)
            self.add_to_network(current, self.href_to_num[i])

    def save_map(self):
        output = open('href_to_num', 'w')
        for i in self.href_to_num:
            output.write(i + "!!!!" + str(self.href_to_num[i]) + "\n")
        output.close()

    def save_graph(self):
        output = open('graph', 'w')
        for i in self.out_link_network:
            output.write(str(i) + ':')
            for j in self.out_link_network[i]:
                if j == i:
                    continue
                output.write(str(j) + ':')
            output.write('\n')
        output.close()
    
    def run(self):
        root_dir = '.'
        html_suffix = '.html'

        all_data = os.walk(root_dir)
        cnt = 0
        for i in all_data:
            cnt = cnt + 1
            if cnt % 500 == 0:
                print cnt

            for j in i[2]:
                # j is a filename
                suffix = os.path.splitext(j)[1]
                if suffix == html_suffix:
                    self.parse_file(i[0] + os.sep + j)
        self.save_map()
        self.save_graph()

def main():
    #prp = PRprocess()
    #prp.run()
    pc = PRcalc()
    pc.run()
    
if __name__ == '__main__':
    main()
