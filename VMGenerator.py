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
   
def vm(id, mips, size='1e4', ram='512', bw='1e3', pesNumber='1', vmm='Xen', CloudletScheduler='SubtaskScheduler'):
	v = ET.Element('VM', ref=str(id))
	ET.SubElement(v, 'size').text = str(size)
	ET.SubElement(v, 'ram').text = str(ram)
	ET.SubElement(v, 'mips').text = str(mips)
	ET.SubElement(v, 'bw').text = str(bw)
	ET.SubElement(v, 'pesNumber').text = str(pesNumber)
	ET.SubElement(v, 'vmm').text = str(vmm)
	ET.SubElement(v, 'CloudletScheduler').text = str(CloudletScheduler)
	return v

def main():
	
	parser = argparse.ArgumentParser()
	parser.add_argument('--seed', type=parsePositiveInteger, help='the seed for random number generator')
	parser.add_argument('--numV', type=parsePositiveInteger, help='the number of VMs', default=4)
	parser.add_argument('--size', type=parsePositiveInteger, help='the size of VM image', default=1e4)
	parser.add_argument('--ram', type=parsePositiveInteger, help='the size of ram', default=512)
	parser.add_argument('--mipsR', type=parseRange, help='the range of the mips', default='1e3,1e4')
	parser.add_argument('--bw', type=parsePositiveInteger, help='the bandwidth', default=1e3)
	parser.add_argument('--pesN', type=parsePositiveInteger, help='the number of processing elements (cores)', default=1)
	
	args = parser.parse_args()
	
	if args.seed<>None:
		random.seed(args.seed)
	
	root = ET.Element('Customer')
	
	for vmId in xrange(args.numV):
		v = vm(vmId,random.randint(args.mipsR[0], args.mipsR[1]))
		root.append(v)
	
	print prettify(root)

if __name__ == '__main__':
	main()

	