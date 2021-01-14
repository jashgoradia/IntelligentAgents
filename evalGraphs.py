import os
import sys
from pprint import pprint
import matplotlib.pyplot as plt

negotiationResults = []

def getPath(subDir):
    return os.path.join(os.getcwd(), "genius-9.1.11", "logs", subDir)

def readLogs(subDir):
    agreement = {"Yes": 1, "No": 0}
    pathToLogs = getPath(subDir)
    cnt = 0
    for filename in os.listdir(pathToLogs):
        if os.path.isdir(os.path.join(pathToLogs, filename)):
            continue
        domain, agent = filename.split('-')[1:]
        with open(os.path.join(pathToLogs, filename), 'r') as f:
            file = f.read().splitlines()
            for line in file[2:]:
                negotiation = line.split(";")
                results = {
                    "Domain": domain,
                    "Agent": agent.split('.csv')[0].split('.')[-1],
                    "Rounds": negotiation[1],
                    "Agreement": agreement[negotiation[4]],
                    "DistanceToNash": negotiation[10],
                    "SocialWelfare": negotiation[11],
                }
                if cnt % 2 == 0:
                    results["Utility"] = float(negotiation[14][:5])
                    results["StartingPosition"] = "#ff0000"
                else:
                    results["Utility"] = float(negotiation[15][:5])
                    results["StartingPosition"] = "#0000ff"
                cnt += 1
                negotiationResults.append(results)

def printGraphsByDomain(subDir):
    domains = set([d["Domain"] for d in negotiationResults])
    save = len(sys.argv) > 2
    graphDirPath = os.path.join(getPath(subDir), "graphs")
    if save and (not os.path.isdir(graphDirPath)):
        os.mkdir(graphDirPath)
    for domain in domains:
        negotiationsInDomain = [d for d in negotiationResults if d["Domain"] == domain]
        names = [d["Agent"] for d in negotiationsInDomain]
        values = [d["Utility"] for d in negotiationsInDomain]
        colors = [d["StartingPosition"] for d in negotiationsInDomain]
        print(values)
        plt.scatter(names, values, c=colors)
        plt.plot(values[::2], c="#ff0000", label="Our agent goes first")
        plt.plot(values[1::2], c="#0000ff", label="The other agent goes first")
        plt.legend(loc="upper left")
        plt.title(domain)
        if (len(sys.argv) > 2):
            plt.savefig(os.path.join(graphDirPath, '{}.png'.format(domain)))
            plt.clf()
        else:
            plt.show()


if __name__ == '__main__':
    readLogs(sys.argv[1])
    printGraphsByDomain(sys.argv[1])
