#!/usr/bin/env python

'''A library and a command line tool to interact with the LOCKSS daemon status
service via its Web Services API.'''

__copyright__ = '''\
Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.
'''

__license__ = '''\
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.
'''

__version__ = '0.4.0'

import getpass
import itertools
import optparse
import os.path
import sys

import DaemonStatusServiceImplService_client
from wsutil import datetimems, datems, durationms, zsiauth

#
# Library
#

def get_au_status(host, auth, auid):
  '''Performs a getAuStatus operation on the given host for the given AUID, and
  returns a record with these fields:
  - AccessType (string)
  - AvailableFromPublisher (boolean)
  - ContentSize (numeric)
  - CrawlPool (string)
  - CrawlProxy (string)
  - CrawlWindow (string)
  - CreationTime (numeric)
  - CurrentlyCrawling (boolean)
  - CurrentlyPolling (boolean)
  - DiskUsage (numeric)
  - JournalTitle (string)
  - LastCompletedCrawl (numeric)
  - LastCompletedPoll (numeric)
  - LastCrawl (numeric)
  - LastCrawlResult (string)
  - LastPoll (numeric)
  - LastPollResult (string)
  - PluginName (string)
  - Provider (string)
  - Publisher (string)
  - PublishingPlatform (string)
  - RecentPollAgreement (floating point)
  - Repository (string)
  - Status (string)
  - SubscriptionStatus (string)
  - SubstanceState (string)
  - Volume (string) (the AU name)
  - Year (string)
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auid (string): an AUID
  '''
  req = DaemonStatusServiceImplService_client.getAuStatus()
  req.AuId = auid
  return _ws_port(host, auth).getAuStatus(req).Return

def get_au_urls(host, auth, auid, prefix=None):
  '''Performs a getAuUrls operation on the given host for the given AUID and
  returns a list of URLs (strings) in the AU. If the optional prefix argument is
  given, limits the results to URLs with that prefix (including the URL itself).

  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auid (string): an AUID
  - prefix (string): a URL prefix (default: None)
  '''
  req = DaemonStatusServiceImplService_client.getAuUrls()
  req.AuId = auid
  if prefix is not None: req.url = prefix
  return _ws_port(host, auth).getAuUrls(req).Return

def get_auids(host, auth):
  '''Performs a getAuids operation on the given host, which really produces a
  sequence of all AUIDs with the AU names, and returns a list of records with
  these fields:
  - Id (string)
  - Name (string)
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  '''
  req = DaemonStatusServiceImplService_client.getAuIds()
  return _ws_port(host, auth).getAuIds(req).Return

def get_peer_agreements(host, auth, auid):
  '''Convenience call to query_aus() that returns the PeerAgreements list for
  the given AUID (or None if there is no such AUID). The PeerAgreements list is
  a list of records with these fields:
  - Agreements, a record with these fields:
      - Entry, a list of records with these fields:
          - Key, a string among:
              - "POR"
              - "POP"
              - "SYMMETRIC_POR"
              - "SYMMETRIC_POP"
              - "POR_HINT"
              - "POP_HINT"
              - "SYMMETRIC_POR_HINT"
              - "SYMMETRIC_POP_HINT"
              - "W_POR"
              - "W_POP"
              - "W_SYMMETRIC_POR"
              - "W_SYMMETRIC_POP"
              - "W_POR_HINT"
              - "W_POP_HINT"
              - "W_SYMMETRIC_POR_HINT"
              - "W_SYMMETRIC_POP_HINT"
          - Value, a record with these fields:
              - HighestPercentAgreement (floating point)
              - HighestPercentAgreementTimestamp (numeric)
              - PercentAgreement (floating point)
              - PercentAgreementTimestamp (numeric)
  - PeerId (string)
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auid (string): an AUID
  '''
  res = query_aus(host, auth, 'peerAgreements', 'auId = "%s"' % (auid,))
  if len(res) == 0: return None
  else: return res[0].PeerAgreements

def get_platform_configuration(host, auth):
  '''Performs a getPlatformConfiguration operation on the given host and returns
  a record with these fields:
  - AdminEmail (string)
  - BuildHost (string)
  - BuildTimestamp (numeric)
  - CurrentTime (numeric)
  - CurrentWorkingDirectory (string)
  - DaemonVersion, a record with these fields:
      - BuildVersion (numeric)
      - FullVersion (string)
      - MajorVersion (numeric)
      - MinorVersion (numeric)
  - Disks (list of strings)
  - Groups (list of strings)
  - HostName (string)
  - IpAddress (string)
  - JavaVersion, a record with these fields:
      - RuntimeName (string)
      - RuntimeVersion (string)
      - SpecificationVersion (string)
      - Version (string)
  - MailRelay (string)
  - Platform, a record with these fields:
      - Name (string)
      - Suffix (string)
      - Version (string)
  - Project (string)
  - Properties (list of strings)
  - Uptime (numeric)
  - V3Identity (string)
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  '''
  req = DaemonStatusServiceImplService_client.getPlatformConfiguration()
  return _ws_port(host, auth).getPlatformConfiguration(req).Return

def is_daemon_ready(host, auth):
  '''Performs an isDaemonReady operation on the given host and returns True or
  False.
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  '''
  req = DaemonStatusServiceImplService_client.isDaemonReady()
  return _ws_port(host, auth).isDaemonReady(req).Return

def query_aus(host, auth, select, where=None):
  '''Performs a queryAus operation on the given host, using the given field
  names to build a SELECT clause, optionally using the given string to build a
  WHERE clause, and returns a list of records with these fields (populated or
  not depending on the SELECT clause):
  - AccessType (string)
  - ArticleUrls (list of strings)
  - AuConfiguration, a record with these fields:
      - DefParams, a list of records with these fields:
          - Key (string)
          - Value (string)
      - NonDefParams, a list of records with these fields:
          - Key (string)
          - Value (string)
  - AuId (string)
  - AvailableFromPublisher (boolean)
  - ContentSize (numeric)
  - CrawlPool (string)
  - CrawlProxy (string)
  - CrawlWindow (string)
  - CreationTime (numeric)
  - CurrentlyCrawling (boolean)
  - CurrentlyPolling (boolean)
  - DiskUsage (numeric)
  - HighestPollAgreement (numeric)
  - IsBulkContent (boolean)
  - JournalTitle (string)
  - LastCompletedCrawl (numeric)
  - LastCompletedPoll (numeric)
  - LastCrawl (numeric)
  - LastCrawlResult (string)
  - LastPoll (numeric)
  - LastPollResult (string)
  - Name (string)
  - NewContentCrawlUrls (list of strings)
  - PeerAgreements, a list of records with these fields:
      - Agreements, a record with these fields:
          - Entry, a list of records with these fields:
              - Key, a string among:
                  - "POR"
                  - "POP"
                  - "SYMMETRIC_POR"
                  - "SYMMETRIC_POP"
                  - "POR_HINT"
                  - "POP_HINT"
                  - "SYMMETRIC_POR_HINT"
                  - "SYMMETRIC_POP_HINT"
                  - "W_POR"
                  - "W_POP"
                  - "W_SYMMETRIC_POR"
                  - "W_SYMMETRIC_POP"
                  - "W_POR_HINT"
                  - "W_POP_HINT"
                  - "W_SYMMETRIC_POR_HINT"
                  - "W_SYMMETRIC_POP_HINT"
              - Value, a record with these fields:
                  - HighestPercentAgreement (floating point)
                  - HighestPercentAgreementTimestamp (numeric)
                  - PercentAgreement (floating point)
                  - PercentAgreementTimestamp (numeric)
      - PeerId (string)
  - PluginName (string)
  - PublishingPlatform (string)
  - RecentPollAgreement (numeric)
  - RepositoryPath (string)
  - SubscriptionStatus (string)
  - SubstanceState (string)
  - TdbProvider (string)
  - TdbPublisher (string)
  - TdbYear (string)
  - UrlStems (list of strings)
  - Urls, a list of records with these fields:
      - CureentVersionSize (numeric)
      - Url (string)
      - VersionCount (numeric)
  - Volume (string)
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - select (string or list of strings): if a list of strings, the field names to
  be used in the SELECT clause; if a string, the single field name to be used in
  the SELECT clause
  - where (string): optional statement for the WHERE clause (default: None)
  Raises:
  - ValueError if select is not of the right type
  '''
  if type(select) is list: query = 'SELECT %s' % (', '.join(select))
  elif type(select) is str: query = 'SELECT %s' % (select,)
  else: raise ValueError, 'invalid type for select parameter: %s' % (type(select),)
  if where is not None: query = '%s WHERE %s' % (query, where)
  req = DaemonStatusServiceImplService_client.queryAus()
  req.AuQuery = query
  return _ws_port(host, auth).queryAus(req).Return

def query_crawls(host, auth, select, where=None):
  '''Performs a queryCrawls operation on the given host, using the given field
  names to build a SELECT clause, optionally using the given string to build a
  WHERE clause, and returns a list of records with these fields (populated or
  not depending on the SELECT clause):
  - AuId (string)
  - AuName (string)
  - BytesFetchedCount (long)
  - CrawlKey (string)
  - CrawlStatus (string)
  - CrawlType (string)
  - Duration (long)
  - LinkDepth (int)
  - MimeTypeCount (int)
  - MimeTypes (list of strings)
  - OffSiteUrlsExcludedCount (int)
  - PagesExcluded (list of strings)
  - PagesExcludedCount (int)
  - PagesFetched (list of strings)
  - PagesFetchedCount (int)
  - PagesNotModified (list of strings)
  - PagesNotModifiedCount (int)
  - PagesParsed (list of strings)
  - PagesParsedCount (int)
  - PagesPending (list of strings)
  - PagesPendingCount (int)
  - PagesWithErrors, a list of records with these fields:
      - Message (string)
      - Severity (string)
      - Url (string)
  - PagesWithErrorsCount (int)
  - RefetchDepth (int)
  - Sources (list of strings)
  - StartTime (long)
  - StartingUrls (list of strings)
  '''
  if type(select) is list: query = 'SELECT %s' % (', '.join(select))
  elif type(select) is str: query = 'SELECT %s' % (select,)
  else: raise ValueError, 'invalid type for select parameter: %s' % (type(select),)
  if where is not None: query = '%s WHERE %s' % (query, where)
  req = DaemonStatusServiceImplService_client.queryCrawls()
  req.CrawlQuery = query
  return _ws_port(host, auth).queryCrawls(req).Return

def _ws_port(host, auth, tracefile=None):
  url = 'http://%s/ws/DaemonStatusService' % (host,)
  locator = DaemonStatusServiceImplService_client.DaemonStatusServiceImplServiceLocator()
  if tracefile is None: return locator.getDaemonStatusServiceImplPort(url=url, auth=auth)
  else: return locator.getDaemonStatusServiceImplPort(url=url, auth=auth, tracefile=tracefile)

#
# Command line tool
#

class _DaemonStatusServiceOptions(object):

  @staticmethod
  def make_parser():
    usage = '%prog {--host=HOST|--hosts=HFILE}... [OPTIONS]'
    parser = optparse.OptionParser(version=__version__, description=__doc__, usage=usage)
    # Hosts
    group = optparse.OptionGroup(parser, 'Target hosts')
    group.add_option('--host', action='append', default=list(), help='add host:port pair to list of target hosts')
    group.add_option('--hosts', action='append', default=list(), metavar='HFILE', help='add host:port pairs in HFILE to list of target hosts')
    group.add_option('--password', metavar='PASS', help='UI password (default: interactive prompt)')
    group.add_option('--username', metavar='USER', help='UI username (default: interactive prompt)')
    parser.add_option_group(group)
    # AUIDs
    group = optparse.OptionGroup(parser, 'Target AUIDs')
    group.add_option('--auid', action='append', default=list(), help='add AUID to list of target AUIDs')
    group.add_option('--auids', action='append', default=list(), metavar='AFILE', help='add AUIDs in AFILE to list of target AUIDs')
    parser.add_option_group(group)
    # Daemon operations
    group = optparse.OptionGroup(parser, 'Daemon operations')
    group.add_option('--get-platform-configuration', action='store_true', help='output platform configuration information for target hosts; narrow down with optional --select list chosen among %s' % (', '.join(sorted(_PLATFORM_CONFIGURATION)),))
    group.add_option('--is-daemon-ready', action='store_true', help='output True/False table of ready status of target hosts; always exit with 0')
    group.add_option('--is-daemon-ready-quiet', action='store_true', help='output nothing; exit with 0 if all target hosts are ready, 1 otherwise')
    parser.add_option_group(group)
    # AUID operations
    group = optparse.OptionGroup(parser, 'AU operations')
    group.add_option('--get-au-status', action='store_true', help='output status information about target AUIDs; narrow down output with optional --select list chosen among %s' % (', '.join(sorted(_AU_STATUS)),))
    group.add_option('--get-au-urls', action='store_true', help='output URLs in one AU on one host')
    group.add_option('--get-auids', action='store_true', help='output True/False table of all AUIDs (or target AUIDs if specified) present on target hosts')
    group.add_option('--get-auids-names', action='store_true', help='output True/False table of all AUIDs (or target AUIDs if specified) and their names present on target hosts')
    group.add_option('--get-peer-agreements', action='store_true', help='output peer agreements for one AU on one hosts')
    group.add_option('--query-aus', action='store_true', help='perform AU query (with optional --where clause) with --select list chosen among %s' % (', '.join(sorted(_QUERY_AUS)),))
    parser.add_option_group(group)
    # Crawl operations
    group = optparse.OptionGroup(parser, 'Crawl operations')
    group.add_option('--query-crawls', action='store_true', help='perform crawl query (with optional --where clause) with --select list chosen among %s' % (', '.join(sorted(_QUERY_CRAWLS)),))
    parser.add_option_group(group)
    # Other
    group = optparse.OptionGroup(parser, 'Other options')
    group.add_option('--group-by-field', action='store_true', help='group results by field instead of host')
    group.add_option('--no-special-output', action='store_true', help='no special output format for a single target host')
    group.add_option('--select', metavar='FIELDS', help='comma-separated list of fields for narrower output')
    group.add_option('--where', help='optional WHERE clause for query operations')
    parser.add_option_group(group)
    return parser

  def __init__(self, parser, opts, args):
    super(_DaemonStatusServiceOptions, self).__init__()
    if len(args) > 0: parser.error('extraneous arguments: %s' % (' '.join(args)))
    if len(filter(None, [opts.get_au_status, opts.get_au_urls, opts.get_auids, opts.get_auids_names, opts.get_peer_agreements, opts.get_platform_configuration, opts.is_daemon_ready, opts.is_daemon_ready_quiet, opts.query_aus, opts.query_crawls])) != 1:
      parser.error('exactly one of --get-au-status, --get-au-urls, --get-auids, --get-auids-names, --get-peer-agreements, --get-platform-configuration, --is-daemon-ready, --is-daemon-ready-quiet, --query-aus --query-crawls is required')
    if len(opts.auid) + len(opts.auids) > 0 and not any([opts.get_au_status, opts.get_au_urls, opts.get_auids, opts.get_auids_names, opts.get_peer_agreements]):
      parser.error('--auid, --auids can only be applied to --get-au-status, --get-au-urls, --get-auids, --get-auids-names, --get-peer-agreements')
    if opts.select and not any([opts.get_au_status, opts.get_platform_configuration, opts.query_aus, opts.query_crawls]):
      parser.error('--select can only be applied to --get-au-status, --get-platform-configuration, --query-aus, --query-crawls')
    if opts.where and not any([opts.query_au, opts.query_crawls]):
      parser.error('--where can only be applied to --query-aus, --query-crawls')
    if opts.group_by_field and not any([opts.get_au_status, opts.query_aus]):
      parser.error('--group-by-field can only be applied to --get-au-status, --query-aus')
    # hosts
    self.hosts = opts.host[:]
    for f in opts.hosts: self.hosts.extend(_file_lines(f))
    if len(self.hosts) == 0: parser.error('at least one target host is required')
    # auids
    self.auids = opts.auid[:]
    for f in opts.auids: self.auids.extend(_file_lines(f))
    # get_auids/get_auids_names/is_daemon_ready/is_daemon_ready_quiet
    self.get_auids = opts.get_auids
    self.get_auids_names = opts.get_auids_names
    self.is_daemon_ready = opts.is_daemon_ready
    self.is_daemon_ready_quiet = opts.is_daemon_ready_quiet
    # get_platform_configuration/select
    self.get_platform_configuration = opts.get_platform_configuration
    if self.get_platform_configuration:
      self.select = self.__init_select(parser, opts, _PLATFORM_CONFIGURATION)
    # get_au_status/select
    self.get_au_status = opts.get_au_status
    if self.get_au_status:
      if len(self.auids) == 0: parser.error('at least one target AUID is required with --get-au-status')
      self.select = self.__init_select(parser, opts, _AU_STATUS)
    # get_au_urls
    self.get_au_urls = opts.get_au_urls
    if self.get_au_urls:
      if len(self.hosts) != 1: parser.error('only one target host is allowed with --get-au-urls')
      if len(self.auids) != 1: parser.error('only one target AUID is allowed with --get-au-urls')
    # get_peer_agreements
    self.get_peer_agreements = opts.get_peer_agreements
    if self.get_peer_agreements:
      if len(self.hosts) != 1: parser.error('only one target host is allowed with --get-peer-agreements')
      if len(self.auids) != 1: parser.error('only one target AUID is allowed with --get-peer-agreements')
    # query_aus/select/where
    self.query_aus = opts.query_aus
    if self.query_aus:
      self.select = self.__init_select(parser, opts, _QUERY_AUS)
      self.where = opts.where
    # query_crawls/select/where
    self.query_crawls = opts.query_crawls
    if self.query_crawls:
      if len(self.hosts) != 1: parser.error('only one target host is allowed with --query-crawls')
      self.select = self.__init_select(parser, opts, _QUERY_CRAWLS)
      self.where = opts.where
    # group_by_field/no_special_output
    self.group_by_field = opts.group_by_field
    self.no_special_output = opts.no_special_output
    # auth
    u = opts.username or getpass.getpass('UI username: ')
    p = opts.password or getpass.getpass('UI password: ')
    self.auth = zsiauth(u, p)

  def __init_select(self, parser, opts, field_dict):
    if opts.select is None: return sorted(field_dict)
    fields = [s.strip() for s in opts.select.split(',')]
    errfields = filter(lambda f: f not in field_dict, fields)
    if len(errfields) == 1: parser.error('unknown field: %s' % (errfields[0],))
    if len(errfields) > 1: parser.error('unknown fields: %s' % (', '.join(errfields),))
    return fields

# Last modified 2015-08-05
def _output_record(options, lst):
  print '\t'.join([str(x or '') for x in lst])

# Last modified 2015-08-05
def _output_table(options, data, rowheaders, lstcolkeys):
  colkeys = [x for x in itertools.product(*lstcolkeys)]
  for j in xrange(len(lstcolkeys)):
    if j < len(lstcolkeys) - 1: rowpart = [''] * len(rowheaders)
    else: rowpart = rowheaders
    _output_record(options, rowpart + [x[j] for x in colkeys])
  for rowkey in sorted(set([k[0] for k in data])):
    _output_record(options, list(rowkey) + [data.get((rowkey, colkey)) for colkey in colkeys])

_AU_STATUS = {
  'accessType': ('Access type', lambda r: r.AccessType),
  'availableFromPublisher': ('Available from publisher', lambda r: r.AvailableFromPublisher),
  'contentSize': ('Content size', lambda r: r.ContentSize),
  'crawlPool': ('Crawl pool', lambda r: r.CrawlPool),
  'crawlProxy': ('Crawl proxy', lambda r: r.CrawlProxy),
  'crawlWindow': ('Crawl window', lambda r: r.CrawlWindow),
  'creationTime': ('Creation time', lambda r: datetimems(r.CreationTime)),
  'currentlyCrawling': ('Currently crawling', lambda r: r.CurrentlyCrawling),
  'currentlyPolling': ('Currently polling', lambda r: r.CurrentlyPolling),
  'diskUsage': ('Disk usage', lambda r: r.DiskUsage),
  'journalTitle': ('Journal title', lambda r: r.JournalTitle),
  'lastCompletedCrawl': ('Last completed crawl', lambda r: datetimems(r.LastCompletedCrawl)),
  'lastCompletedPoll': ('Last completed poll', lambda r: datetimems(r.LastCompletedPoll)),
  'lastCrawl': ('Last crawl', lambda r: datetimems(r.LastCrawl)),
  'lastCrawlResult': ('Last crawl result', lambda r: r.LastCrawlResult),
  'lastPoll': ('Last poll', lambda r: datetimems(r.LastPoll)),
  'lastPollResult': ('Last poll result', lambda r: r.LastPollResult),
  'pluginName': ('Plugin name', lambda r: r.PluginName),
  'provider': ('Provider', lambda r: r.Provider),
  'publisher': ('Publisher', lambda r: r.Publisher),
  'publishingPlatform': ('Publishing platform', lambda r: r.PublishingPlatform),
  'recentPollAgreement': ('Recent poll agreement', lambda r: r.RecentPollAgreement),
  'repository': ('Repository', lambda r: r.Repository),
  'status': ('Status', lambda r: r.Status),
  'subscriptionStatus': ('Subscription status', lambda r: r.SubscriptionStatus),
  'substanceState': ('Substance state', lambda r: r.SubstanceState),
  'volume': ('Volume name', lambda r: r.Volume),
  'year': ('Year', lambda r: r.Year)
}

def _do_get_au_status(options):
  data = dict()
  for host in options.hosts:
    hostauids = set([r.Id for r in get_auids(host, options.auth)])
    for auid in options.auids:
      if auid not in hostauids: continue
      r = get_au_status(host, options.auth, auid)
      if r is None: continue # Probably doesn't happen
      for x in options.select:
        head, lamb = _AU_STATUS[x]
        data[(auid, host, head)] = lamb(r)
  if options.group_by_field:
    display = dict([(((k[0],), (k[2], k[1])), v) for k, v in data.iteritems()])
    _output_table(options, display, ['AUID'], [[_AU_STATUS[k][0] for k in options.select], sorted(options.hosts)])
  else:
    display = dict([(((k[0],), (k[1], k[2])), v) for k, v in data.iteritems()])
    _output_table(options, display, ['AUID'], [sorted(options.hosts), [_AU_STATUS[k][0] for k in options.select]])

def _do_get_au_urls(options):
  r = get_au_urls(options.hosts[0], options.auth, options.auids[0])
  for url in sorted(r): _output_record(options, [url])

def _do_get_auids(options):
  _do_get_auids_names(options, False)

def _do_get_auids_names(options, do_names=True):
  data = set()
  names = dict()
  for host in options.hosts:
    for r in get_auids(host, options.auth):
      data.add((r.Id, host))
      if do_names: names.setdefault(r.Id, r.Name)
  # Special case
  if len(options.hosts) == 1 and len(options.auids) == 0 and not options.no_special_output:
    if do_names:
      for x in sorted(data): _output_record(options, [x[0], names.get(x[0])])
    else:
      for x in sorted(data): _output_record(options, [x[0]])
    return
  if len(options.auids) == 0: auids = set([x[0] for x in data])
  else: auids = options.auids
  if do_names:
    display = dict([(((x[0], names.get(x[0])), (x[1],)), str(x in data)) for x in itertools.product(auids, options.hosts)])
    _output_table(options, display, ['AUID', 'Name'], [sorted(options.hosts)])
  else:
    display = dict([(((x[0],), (x[1],)), str(x in data)) for x in itertools.product(auids, options.hosts)])
    _output_table(options, display, ['AUID'], [sorted(options.hosts)])

def _do_get_peer_agreements(options):
  pa = get_peer_agreements(options.hosts[0], options.auth, options.auids[0])
  if pa is None:
    print 'No such AUID'
    return
  for pae in pa:
    for ae in pae.Agreements.Entry:
      _output_record(options, [pae.PeerId, ae.Key, ae.Value.PercentAgreement, datetimems(ae.Value.PercentAgreementTimestamp), ae.Value.HighestPercentAgreement, datetimems(ae.Value.HighestPercentAgreementTimestamp)])

_PLATFORM_CONFIGURATION = {
  'adminEmail': ('Admin e-mail', lambda r: r.AdminEmail),
  'buildHost':  ('Build host', lambda r: r.BuildHost),
  'buildTimestamp': ('Build timestamp', lambda r: datetimems(r.BuildTimestamp)),
  'currentTime': ('Current time', lambda r: datetimems(r.CurrentTime)),
  'currentWorkingDirectory': ('Current working directory', lambda r: r.CurrentWorkingDirectory),
  'daemonBuildVersion': ('Daemon build version', lambda r: r.DaemonVersion.BuildVersion),
  'daemonFullVersion': ('Daemon full version', lambda r: r.DaemonVersion.FullVersion),
  'daemonMajorVersion': ('Daemon major version', lambda r: r.DaemonVersion.MajorVersion),
  'daemonMinorVersion': ('Daemon minor version', lambda r: r.DaemonVersion.MinorVersion),
  'disks': ('Disks', lambda r: ', '.join(r.Disks)),
  'groups': ('Groups', lambda r: ', '.join(r.Groups)),
  'hostName': ('Host name', lambda r: r.HostName),
  'ipAddress': ('IP address', lambda r: r.IpAddress),
  'javaRuntimeName': ('Java runtime name', lambda r: r.JavaVersion.RuntimeName),
  'javaRuntimeVersion': ('Java runtime version', lambda r: r.JavaVersion.RuntimeVersion),
  'javaSpecificationVersion': ('Java specification version', lambda r: r.JavaVersion.SpecificationVersion),
  'javaVersion': ('Java version', lambda r: r.JavaVersion.Version),
  'mailRelay': ('Mail relay', lambda r: r.MailRelay),
  'platformName': ('Platform name', lambda r: r.Platform.Name),
  'platformSuffix': ('Platform suffix', lambda r: r.Platform.Suffix),
  'platformVersion': ('Platform version', lambda r: r.Platform.Version),
  'project': ('Project', lambda r: r.Project),
  'properties': ('Properties', lambda r: ', '.join(r.Properties)),
  'uptime': ('Uptime', lambda r: durationms(r.Uptime)),
  'v3Identity': ('V3 identity', lambda r: r.V3Identity)
}

def _do_get_platform_configuration(options):
  data = dict()
  for host in options.hosts:
    r = get_platform_configuration(host, options.auth)
    for x in options.select:
      head, lamb = _PLATFORM_CONFIGURATION[x]
      data[(head, host)] = lamb(r)
  # Special case
  if len(options.hosts) == 1 and not options.no_special_output:
    for k, v in sorted(data.iteritems()): _output_record(options, [k[0], v])
    return
  display = dict([(((k[0],), (k[1],)), v) for k, v in data.iteritems()])
  _output_table(options, display, [''], [sorted(options.hosts)])

def _do_is_daemon_ready(options):
  '''Outputs a table whose rows are target hosts and whose column is filled with
  'True' is the host is ready or 'False' otherwise. (If there is a single target
  host and single output is not disabled, only the word 'True' or 'False' is
  displayed.)
  '''
  # Special case
  if len(options.hosts) == 1 and not options.no_special_output:
    _output_record(options, [str(is_daemon_ready(options.hosts[0], options.auth))])
    return
  for host in sorted(options.hosts):
    _output_record(options, [host, str(is_daemon_ready(host, options.auth))])

def _do_is_daemon_ready_quiet(options):
  '''Outputs nothing; exits with 0 if all target hosts are ready, 1
  otherwise.
  '''
  for host in options.hosts:
    if not is_daemon_ready(host, options.auth): sys.exit(1)
  sys.exit(0)

_QUERY_AUS = {
  'accessType': ('Access type', lambda r: r.AccessType),
  'articleUrls': ('Article URLs', lambda r: '<ArticleUrls>'),
  'auConfiguration': ('AU configuration', lambda r: '<AuConfiguration>'),
  'auId': ('AUID', lambda r: r.AuId),
  'availableFromPublisher': ('Available from publisher', lambda r: r.AvailableFromPublisher),
  'contentSize': ('Content size', lambda r: r.ContentSize),
  'crawlPool': ('Crawl pool', lambda r: r.CrawlPool),
  'crawlProxy': ('Crawl proxy', lambda r: r.CrawlProxy),
  'crawlWindow': ('Crawl window', lambda r: r.CrawlWindow),
  'creationTime': ('Creation time', lambda r: datetimems(r.CreationTime)),
  'currentlyCrawling': ('Currently crawling', lambda r: r.CurrentlyCrawling),
  'currentlyPolling': ('Currently polling', lambda r: r.CurrentlyPolling),
  'diskUsage': ('Disk usage', lambda r: r.DiskUsage),
  'highestPollAgreement': ('Highest poll agreement', lambda r: r.HighestPollAgreement),
  'isBulkContent': ('Is bulk content', lambda r: r.IsBulkContent),
  'journalTitle': ('Title', lambda r: r.JournalTitle),
  'lastCompletedCrawl': ('Last completed crawl', lambda r: datetimems(r.LastCompletedCrawl)),
  'lastCompletedPoll': ('Last completed poll', lambda r: datetimems(r.LastCompletedPoll)),
  'lastCrawl': ('Last crawl', lambda r: datetimems(r.LastCrawl)),
  'lastCrawlResult': ('Last crawl result', lambda r: r.LastCrawlResult),
  'lastPoll': ('Last poll', lambda r: datetimems(r.LastPoll)),
  'lastPollResult': ('Last poll result', lambda r: r.LastPollResult),
  'name': ('Name', lambda r: r.Name),
  'newContentCrawlUrls': ('New content crawl URLs', '<NewContentCrawlUrls>'),
  'peerAgreements': ('Peer agreements', lambda r: '<PeerAgreements>'),
  'pluginName': ('Plugin name', lambda r: r.PluginName),
  'publishingPlatform': ('Publishing platform', lambda r: r.PublishingPlatform),
  'recentPollAgreement': ('Recent poll agreement', lambda r: r.RecentPollAgreement),
  'repositoryPath': ('Repository path', lambda r: r.RepositoryPath),
  'subscriptionStatus': ('Subscription status', lambda r: r.SubscriptionStatus),
  'substanceState': ('Substance state', lambda r: r.SubstanceState),
  'tdbProvider': ('TDB provider', lambda r: r.TdbProvider),
  'tdbPublisher': ('TDB publisher', lambda r: r.TdbPublisher),
  'tdbYear': ('TDB year', lambda r: r.TdbYear),
  'urlStems': ('URL stems', lambda r: '<UrlStems>'),
  'urls': ('URLs', lambda r: '<Urls>'),
  'volume': ('Volume', lambda r: r.Volume)
}

def _do_query_aus(options):
  select = filter(lambda x: x != 'auId', options.select)
  data = dict()
  for host in options.hosts:
    for r in query_aus(host, options.auth, ['auId'] + select, options.where):
      auid = r.AuId
      for x in select:
        head, lamb = _QUERY_AUS[x]      
        data[(auid, host, head)] = lamb(r)
  if options.group_by_field:
    display = dict([(((k[0],), (k[2], k[1])), v) for k, v in data.iteritems()])
    _output_table(options, display, ['AUID'], [[_QUERY_AUS[k][0] for k in select], sorted(options.hosts)])
  else:
    display = dict([(((k[0],), (k[1], k[2])), v) for k, v in data.iteritems()])
    _output_table(options, display, ['AUID'], [sorted(options.hosts), [_QUERY_AUS[k][0] for k in select]])

_QUERY_CRAWLS = {
  'auId': ('AUID', lambda r: r.AuId),
  'auName': ('AU name', lambda r: r.AuName),
  'bytesFetchedCount': ('Bytes Fetched', lambda r: r.BytesFetchedCount),
  'crawlKey': ('Crawl key', lambda r: r.CrawlKey),
  'crawlStatus': ('Crawl status', lambda r: r.CrawlStatus),
  'crawlType': ('Crawl type', lambda r: r.CrawlType),
  'duration': ('Duration', lambda r: durationms(r.Duration)),
  'linkDepth': ('Link depth', lambda r: r.LinkDepth),
  'mimeTypeCount': ('MIME type count', lambda r: r.MimeTypeCount),
  'mimeTypes': ('MIME types', lambda r: '<MIME types>'),
  'offSiteUrlsExcludedCount': ('Off-site URLs excluded count', lambda r: r.OffSiteUrlsExcludedCount),
  'pagesExcluded': ('Pages excluded', lambda r: '<Pages excluded>'),
  'pagesExcludedCount': ('Pages excluded count', lambda r: r.PagesExcludedCount),
  'pagesFetched': ('Pages fetched', lambda r: '<Pages fetched>'),
  'pagesFetchedCount': ('Pages fetched count', lambda r: r.PagesFetchedCount),
  'pagesNotModified': ('Pages not modified', lambda r: '<Pages not modified>'),
  'pagesNotModifiedCount': ('Pages not modified count', lambda r: r.PagesNotModifiedCount),
  'pagesParsed': ('Pages parsed', lambda r: '<Pages parsed>'),
  'pagesParsedCount': ('Pages parsed count', lambda r: r.PagesParsedCount),
  'pagesPending': ('Pages pending', lambda r: '<Pages pending>'),
  'pagesPendingCount': ('Pages pending count', lambda r: r.PagesPendingCount),
  'pagesWithErrors': ('Pages with errors', lambda r: '<Pages with errors>'),
  'pagesWithErrorsCount': ('Pages with errors count', lambda r: r.PagesWithErrors),
  'refetchDepth': ('RefetchDepth', lambda r: r.RefetchDepth),
  'sources': ('Sources', lambda r: '<Sources>'),
  'startTime': ('Start time', lambda r: datetimems(r.StartTime)),
  'startingUrls': ('Starting URLs', lambda r: '<Starting URLs>')
}

def _do_query_crawls(options):
  select = filter(lambda x: x != 'auId', options.select)
  data = dict()
  for r in query_crawls(options.hosts[0], options.auth, ['auId'] + select, options.where):
    auid = r.AuId
    for x in select:
      head, lamb = _QUERY_CRAWLS[x]
      data[(auid, head)] = lamb(r)
  display = dict([(((k[0],), (k[1],)), v) for k, v in data.iteritems()])
  _output_table(options, display, ['AUID'], [[_QUERY_CRAWLS[k][0] for k in select]])

# Last modified 2015-08-31
def _file_lines(fstr):
  with open(os.path.expanduser(fstr)) as f: ret = filter(lambda y: len(y) > 0, [x.partition('#')[0].strip() for x in f])
  if len(ret) == 0: sys.exit('Error: %s contains no meaningful lines' % (fstr,))
  return ret

def _main():
  '''Main method.'''
  # Parse command line
  parser = _DaemonStatusServiceOptions.make_parser()
  (opts, args) = parser.parse_args()
  options = _DaemonStatusServiceOptions(parser, opts, args)
  # Dispatch
  if options.get_au_status: _do_get_au_status(options)
  elif options.get_au_urls: _do_get_au_urls(options)
  elif options.get_auids: _do_get_auids(options)
  elif options.get_auids_names: _do_get_auids_names(options)
  elif options.get_peer_agreements: _do_get_peer_agreements(options)
  elif options.get_platform_configuration: _do_get_platform_configuration(options)
  elif options.is_daemon_ready: _do_is_daemon_ready(options)
  elif options.is_daemon_ready_quiet: _do_is_daemon_ready_quiet(options)
  elif options.query_aus: _do_query_aus(options)
  elif options.query_crawls: _do_query_crawls(options)
  else: raise RuntimeError, 'Unreachable'

# Main entry point
if __name__ == '__main__': _main()

