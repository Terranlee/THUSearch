import os
import os.path

all_anchor = dict()

def to_file():
    log_file = open('anchor.log', 'w')
    for i in all_anchor:
        if os.path.exists(i):
            where = i.rfind('.')
            write_name = i[:where+1] + 'content'
            output = open(write_name, 'a')
            for j in all_anchor[i]:
                output.write(j.encode('utf-8') + ' ')
            output.write('\n')
            output.close()
        else:
            content = '!!!!can not find anchor file:' + i + "!!!!"
            log_file.write(content.encode('utf-8'))
            for cc in all_anchor[i]:
                log_file.write(cc.encode('utf-8'))
            log_file.write('\n')
    log_file.close()

def append_anchor():
    global all_anchor
    root_dir = ''
    content = open('anchor').readlines()
    for i in content:
        i = i.decode('utf-8')
        two = i.split('!!!!')
        url = two[0].strip()
        anchor = two[1].strip()
        filename = root_dir + url
        if not filename in all_anchor:
            all_anchor[filename] = list()
        all_anchor[filename].append(anchor)

def main():
    append_anchor()
    to_file()

if __name__ == '__main__':
    main()
