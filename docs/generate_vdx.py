import os

def generate_vdx_file():
    output_path = "docs/database_erd.vdx"
    
    # Define VDX template header
    vdx_content = """<?xml version='1.0' encoding='utf-8'?>
<VisioDocument xmlns='http://schemas.microsoft.com/visio/2003/core' xml:space='preserve'>
  <DocumentProperties>
    <Title>Money Manager Unified Database ERD</Title>
    <Subject>Entity Relationship Diagram</Subject>
    <Creator>Money Manager Generator</Creator>
  </DocumentProperties>
  <Pages>
    <Page ID='0' Name='Database ERD'>
      <PageSheet>
        <PageProps>
          <PageWidth>11.0</PageWidth>
          <PageHeight>8.5</PageHeight>
          <DrawingScale>1</DrawingScale>
          <PageScale>1</PageScale>
          <DrawingSizeType>3</DrawingSizeType>
        </PageProps>
      </PageSheet>
      <Shapes>
"""

    # Table Shapes Definitions
    # Coordinates in inches on an 11x8.5 (Landscape) page
    tables = [
        {
            "id": "1",
            "name": "users",
            "x": "2.0", "y": "6.5",
            "w": "2.2", "h": "1.6",
            "text": "users\\n-------------------\\nuser_id [PK, BIGSERIAL]\\nusername [VARCHAR]\\npassword_hash [TEXT]\\ncreated_at [TIMESTAMPTZ]"
        },
        {
            "id": "2",
            "name": "transactions",
            "x": "5.5", "y": "6.5",
            "w": "2.4", "h": "1.8",
            "text": "transactions\\n-------------------\\ntransaction_id [PK]\\nuser_id [FK, BIGINT]\\nname [VARCHAR]\\namount [NUMERIC]\\ncategory [VARCHAR]\\ntx_type [VARCHAR]\\ntx_date [DATE]\\ncreated_at [TIMESTAMPTZ]"
        },
        {
            "id": "3",
            "name": "budgets",
            "x": "9.0", "y": "6.5",
            "w": "2.2", "h": "1.6",
            "text": "budgets\\n-------------------\\nbudget_id [PK]\\nuser_id [FK, BIGINT]\\ncategory [VARCHAR]\\namount_cap [NUMERIC]\\nmonth [SMALLINT]\\nyear [SMALLINT]"
        },
        {
            "id": "4",
            "name": "notes",
            "x": "2.0", "y": "4.0",
            "w": "2.2", "h": "1.4",
            "text": "notes\\n-------------------\\nnote_id [PK]\\nuser_id [FK, BIGINT]\\ntitle [VARCHAR]\\ncontent [TEXT]\\ncreated_at [TIMESTAMPTZ]"
        },
        {
            "id": "5",
            "name": "savings_goals",
            "x": "5.5", "y": "4.0",
            "w": "2.4", "h": "1.6",
            "text": "savings_goals\\n-------------------\\ngoal_id [PK]\\nuser_id [FK, BIGINT]\\nname [VARCHAR]\\ntarget_amount [NUMERIC]\\nsaved_amount [NUMERIC]\\ndeadline [DATE]"
        },
        {
            "id": "6",
            "name": "goal_contributions",
            "x": "9.0", "y": "4.0",
            "w": "2.2", "h": "1.4",
            "text": "goal_contributions\\n-------------------\\ncontribution_id [PK]\\ngoal_id [FK, BIGINT]\\namount [NUMERIC]\\nnote [VARCHAR]\\ncontributed_at [TIMESTAMPTZ]"
        },
        {
            "id": "7",
            "name": "monthly_balance",
            "x": "2.0", "y": "1.5",
            "w": "2.2", "h": "1.4",
            "text": "monthly_balance\\n-------------------\\nbalance_id [PK]\\nuser_id [FK, BIGINT]\\nmonth [SMALLINT]\\nyear [SMALLINT]\\ntotal_amount [NUMERIC]"
        },
        {
            "id": "8",
            "name": "user_settings",
            "x": "5.5", "y": "1.5",
            "w": "2.2", "h": "1.2",
            "text": "user_settings\\n-------------------\\nuser_id [PK, FK, BIGINT]\\nmonthly_income [NUMERIC]"
        }
    ]

    for table in tables:
        vdx_content += f"""        <!-- Table Shape: {table['name']} -->
        <Shape ID='{table['id']}' Type='Shape'>
          <XForm>
            <PinX>{table['x']}</PinX>
            <PinY>{table['y']}</PinY>
            <Width>{table['w']}</Width>
            <Height>{table['h']}</Height>
          </XForm>
          <Text>{table['text']}</Text>
        </Shape>
"""

    # Add Connectors (Representing Foreign Key constraints in Visio)
    # Connector IDs start at 100
    connectors = [
        # users(1) -> transactions(2)
        {"id": "101", "from": "1", "to": "2", "text": "user_id"},
        # users(1) -> budgets(3)
        {"id": "102", "from": "1", "to": "3", "text": "user_id"},
        # users(1) -> notes(4)
        {"id": "103", "from": "1", "to": "4", "text": "user_id"},
        # users(1) -> savings_goals(5)
        {"id": "104", "from": "1", "to": "5", "text": "user_id"},
        # savings_goals(5) -> goal_contributions(6)
        {"id": "105", "from": "5", "to": "6", "text": "goal_id"},
        # users(1) -> monthly_balance(7)
        {"id": "106", "from": "1", "to": "7", "text": "user_id"},
        # users(1) -> user_settings(8)
        {"id": "107", "from": "1", "to": "8", "text": "user_id"}
    ]

    for conn in connectors:
        vdx_content += f"""        <!-- Connection: {conn['text']} -->
        <Shape ID='{conn['id']}' Type='Shape'>
          <XForm>
            <Width>1.0</Width>
            <Height>1.0</Height>
          </XForm>
          <Text>{conn['text']} Relation</Text>
        </Shape>
"""

    # Close XML tags
    vdx_content += """      </Shapes>
    </Page>
  </Pages>
</VisioDocument>
"""

    # Write to file
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(vdx_content)
    print(f"[SUCCESS] Visio VDX XML ERD generated and saved to: {output_path}")

if __name__ == "__main__":
    generate_vdx_file()
