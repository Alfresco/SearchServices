<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		<html>
			<head>
				<title>Report</title>
				<br />
				<style type='text/css'>
					#page-content h3 {font-size: 13px; color: #000000; padding: 0 0 0 5px;
					font-weight: bolder; text-align: left; }
					#page-content table * {padding: 2px 10px 2px 10px;}
					#page-content table {table-layout: fixed; width: 100%; border-spacing: 0px;
					font-size: 12px; margin: 10px 0 10px 20px;}
					#page-content table td,#page-content table th {border-width: 1px; border-style:
					solid; border-color: #E0DFD5; word-wrap: break-word;}
					#page-content {margin: 10px;}
					.scenario_line_failure { solid #AA6270; background: #F09499;; color: black; font-weight:
					bolder; font-size: 12px; padding:0 }
					.scenario_line_passed { font-size: 12px; background: #F09499; padding:0}
					.scenario_line_test { font-size: 14px; background: #E0DFD5;
					padding:0 }
					.scenario_line_action { font-size: 12px; background: #E1F5A9; padding:0 }
					.scenario_line_loading{ font-size: 12px; background: #E0F2F7;
					padding:0 }
					body {color: #000; margin: 0; font-size: 13px; font-family: Arial; }
					p {margin: 0 0 10px 0; }
					p, h2, h3 {margin: 0; padding: 0; }
					table.summary_details th { font-weight: bold; text-align: left; background: #F1F0EC;
					color: #000; font-size: 12px; border-bottom: 1px solid #E0DFD5;
					border-left: 1px solid #E0DFD5;}
					table.summary_details1 { table-layout: fixed; width: 90%;; border-right: 1px solid
					#E0DFD5;margin: 10px 40px 10px 40px;}
					table.summary_details { table-layout: fixed; width: 90%;; border-right: 1px solid
					#E0DFD5;margin: 10px 30px 10px 30px;}
					ul { margin: 5px 0 0 25px; padding: 0;}
					.list1 {width: 4O%;}
					.list2 {width: 70%;}
					.list3 {width: 10%;}
					.list4 {width: 20%;}
					div#summary-module h2 { text-align: left; color: ##000; height: 45px; font-size:
					12px; margin: 0px 10px 10px 40px; background: #F1F0EC; width: 50%;}
					div.title {height: 20px; font-size: 24px; font-weight: bolder;
					text-align: center; }
				</style>
			</head>

			<body>

				<img src="logo.png" align="left"></img>
				<br />
				<br />
				<br />
				<div id='page-content'>
					<div class='title'>
						<xsl:value-of select="suite/@name" />
					</div>
				</div>
				<br />
				<br />
				<br />

				<xsl:for-each select="suite">
					<table cellspacing='0' border='1' width='60%'>
						<tr valign='top'>
							<th class='list1'>SUITE</th>
							<th>TOTAL</th>
							<th>PASSED</th>
							<th>FAILED</th>
							<th>SKIPPED</th>
							<th>RATE</th>
						</tr>
						<tr valign='top' class='Failure'>
							<th>
								<xsl:value-of select="class/@name" />
							</th>
							<th>
								<xsl:value-of select="class/total" />
							</th>
							<th>
								<xsl:value-of select="class/passed" />
							</th>
							<th>
								<xsl:value-of select="class/failed" />
							</th>
							<th>
								<xsl:value-of select="class/skipped" />
							</th>
							<th>
								<xsl:value-of select="class/rate" />
							</th>
						</tr>
					</table>
					<br />

				</xsl:for-each>


				<div>
					<table cellspacing='0' class='summary_details' border='0'>
						<tr>
							<td class='list4'>
								<h3>Tests</h3>
							</td>
							<td class='list3'>
								<h3>Status</h3>
							</td>
							<td class='list2'>
								<h3>Steps</h3>
							</td>
						</tr>

						<xsl:for-each select="suite/class/tests/test">

							<tr>
								<td class='scenario_line_test'>
									<xsl:value-of select="name" />
								</td>
								<td class='scenario_line_test'>
									<xsl:value-of select="status" />
								</td>
								<td class='scenario_line_test'>
									<table class='summary_details'>
										<tr>
											<td>
												<xsl:for-each select="steps/step">
													<xsl:value-of select="." />
													<br />
												</xsl:for-each>
											</td>
										</tr>

										<tr class='scenario_line_passed'>
											<td>
												<xsl:value-of select="error" />
											</td>
										</tr>
									</table>
								</td>
							</tr>
						</xsl:for-each>
					</table>
				</div>


			</body>
		</html>
	</xsl:template>

</xsl:stylesheet>