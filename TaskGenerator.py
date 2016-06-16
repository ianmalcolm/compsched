import argparse
import random
import xml.etree.cElementTree as ET
from xml.dom import minidom

def parseProbability(value):
	if float(value) >= 0.0 and float(value) <= 1.0:
		return float(value)
	else:
		parser.error('Incorrect parameter {}!'.format(value))

def parsePositiveInteger(value):
	if int(float(value)) > 0:
		return int(float(value))
	else:
		parser.error('Incorrect parameter {}!'.format(value))
	
def parseRange(value):
	words = value.split(',')
	if len(words) == 2:
		if float(words[0]) < float(words[1]):
			return float(words[0]), float(words[1]),
	parser.error('Incorrect parameter {}!'.format(value))

def prettify(elem):
    '''Return a pretty-printed XML string for the Element.
    '''
    rough_string = ET.tostring(elem, 'utf-8')
    reparsed = minidom.parseString(rough_string)
    return reparsed.toprettyxml(indent='\t')

def task(id, arrivalTime=0, deadline=''):
	t = ET.Element('Task', ref=str(id))
	ET.SubElement(t, 'arrivalTime').text = str(arrivalTime)
	ET.SubElement(t, 'deadline').text = str(deadline)
	return t

def subtask(id, cloudletLength, pesNumber=1, cloudletFileSize=0, cloudletOutputSize=0, utilizationModelCpu='UtilizationModelFull', utilizationModelRam='UtilizationModelFull', utilizationModelBw='UtilizationModelFull', record='true', fileList=''):
	st = ET.Element('Subtask', ref=str(id))
	ET.SubElement(st, 'cloudletLength').text = str(cloudletLength)
	ET.SubElement(st, 'pesNumber').text = str(pesNumber)
	ET.SubElement(st, 'cloudletFileSize').text = str(cloudletFileSize)
	ET.SubElement(st, 'cloudletOutputSize').text = str(cloudletOutputSize)
	ET.SubElement(st, 'utilizationModelCpu').text = str(utilizationModelCpu)
	ET.SubElement(st, 'utilizationModelRam').text = str(utilizationModelRam)
	ET.SubElement(st, 'utilizationModelBw').text = str(utilizationModelBw)
	ET.SubElement(st, 'record').text = str(record)
	ET.SubElement(st, 'fileList').text = str(fileList)
	return st

def dependency(source, destination):
	dep = ET.Element('Dependency', src=str(source), dst=str(destination))
	return dep

def main():
	
	parser = argparse.ArgumentParser()
	parser.add_argument('-s','--seed', type=parsePositiveInteger, help='the seed for random number generator')
	parser.add_argument('-t','--numT', type=parsePositiveInteger, help='the number of tasks', default=1)
	parser.add_argument('-v','--numV', type=parsePositiveInteger, help='the number of vertices', default=10)
	parser.add_argument('-e','--numE', type=parsePositiveInteger, help='the number of edge', default=100)
	parser.add_argument('-l','--lenR', type=parseRange, help='the parseRange of the subtask instruction length', default='100,1e6')
	args = parser.parse_args()
	
	if args.numE > (args.numV * (args.numV - 1) / 2):
		parser.error('Too many edges {}!'.format(args.numE))
	
	if args.seed <> None:
		random.seed(args.seed)
	
	root = ET.Element('Customer')
	
	for taskId in xrange(args.numT):
		t = task(taskId)
		root.append(t)
		for subtaskId in xrange(args.numV):
			st = subtask(subtaskId, random.randint(args.lenR[0], args.lenR[1]))
			t.append(st)
		edges = set()
		while len(edges) < args.numE:
			v1 = random.randint(0, args.numV - 1)
			v2 = random.randint(0, args.numV - 1)
			if v1 > v2:
				edges.add((v2, v1))
			elif v1 < v2:
				edges.add((v1, v2))
		for edge in edges:
			dep = dependency(edge[0], edge[1])
			t.append(dep)

	print prettify(root)

if __name__ == '__main__':
	main()
