import datetime
import subprocess
from sys import argv

def genXmlFile(source, agentName, domain):
	return source.replace("{}", agentName).replace("@", domain[0]).replace("#", domain[1])

def writeFile(source, fileName):
	with open(fileName, "w") as f:
		f.write(source)

def extractDomainName(domain):
	return domain[0].split("/")[-2]

def computeFileName(domain):
	return domain[0].split("/")[-1].split(".")[0]

now = datetime.datetime.now()
runName = "-"
if len(argv) > 1:
	runName += argv[1]
else:
	runName += now.strftime("%x-%X")

geniusCommand = "java -cp \"genius-9.1.13.jar;.\" genius.cli.Runner"
xmlSource = None
with open("leaguePartyDomain.xml", "r") as f:
	xmlSource = f.read()

domains = [
			("anac/y2011/Grocery/Grocery_domain_sam.xml", "anac/y2011/Grocery/Grocery_domain_mary.xml"),
			("partydomain/party1_utility_u500_c001.xml","partydomain/party2_utility_u500_c001.xml"),
			("partydomain/party1_utility_u1000_c001.xml", "partydomain/party2_utility_u1000_c001.xml"),
		]

agents = ["agentgg.AgentGG",
		  "fsega2019.agent.FSEGA2019",
		  "gravity.Gravity",
		  "solveragent.SolverAgent",
		  "winkyagent.winkyAgent"]

for domain in domains:
	for agent in agents:
		print("Running our agent against {} on domain {}".format(agent, extractDomainName(domain)))
		writeFile(genXmlFile(xmlSource, agent, domain), "temp.xml")
		print("{} temp.xml logs/epicEvaluation{}/run-{}-{}".format(geniusCommand, runName, computeFileName(domain), agent))
		result = subprocess.run("{} temp.xml logs/epicEvaluation{}/run-{}-{}".format(geniusCommand, runName, computeFileName(domain), agent), shell=True)