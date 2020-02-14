import itertools 

tokenized = "tokenized"
string = "string"
sortable = "sortable"
cross_locale = "cross-locale"
output_file = "generated_copy_fields.xml"


def find_subsets(s, n):
    return [set(i) for x in range(1, n+1) for i in itertools.combinations(s, x) ] 

def get_copy_field_xml(source, destination):
    return '<copyField source="' + source + '" dest="' + destination+ '" />'

def get_dynamic_field_xml(field, field_type):

    postfix = ""
    if field_type in ("text", "content"):
        postfix = '" type="alfrescoFieldType" indexed="false" stored="true" />'
    else:
        postfix = '" type="alfrescoFieldType" indexed="false" stored="true" multiValued="true" />'

    return '<dynamicField name="'+ field + postfix

def get_field_prefix(field_type):
    if field_type == "text":
        return "text@s_"
    elif field_type == "mltext":
        return "mltext@m_"
    elif field_type == "content":
        return "content@s_" 
    else:
        return "text@m_"
    

def generate_fields(field_type, tokenized, string, cross_locale, sortable):

    prefix = get_field_prefix(field_type)
    field = prefix + "stored_" + ("t" if tokenized else "_") + ("s" if string else "_")  + ("c" if cross_locale else "_") + ("s" if sortable else "_" ) + "@*"
    generated_fields = []
    generated_fields.append(get_dynamic_field_xml(field, field_type))

    if tokenized:
        generated_fields.append(get_copy_field_xml(field, create_tokenized(prefix)))
        if cross_locale:
            generated_fields.append(get_copy_field_xml(field, create_tokenized_cross_locale(prefix)))

    if string:
        generated_fields.append(get_copy_field_xml(field, create_non_tokenized(prefix)))
        if cross_locale:
            generated_fields.append(get_copy_field_xml(field, create_non_tokenized_cross_locale(prefix)))

    return generated_fields

def create_tokenized(prefix):
    return prefix + "_lt" + "@*"

def create_tokenized_cross_locale(prefix):
    return prefix + "__t" + "@*"

def create_non_tokenized(prefix):
    return prefix + "_l_" + "@*"

def create_non_tokenized_cross_locale(prefix):
    return prefix + "___" + "@*"

def create_sortable(prefix):
    return prefix + "_sort" + "@*"

def generate_text(file):
    
    for t in ("text", "mltext", "content", "multivalue-text"):
        s = {tokenized, string, cross_locale, sortable} if t == "text" else {tokenized, string, cross_locale} 
        for s in find_subsets(s, 4):
            generated = generate_fields(t, tokenized in s, string in s, cross_locale in s, sortable in s)
            file.writelines(["%s\n" % item  for item in generated])
            file.write("\n")
        file.write("\n")

def main():
    file = open(output_file, "w")
    file.write('<fields>\n')
    generate_text(file)
    file.write('</fields>')
    file.close()

main()
    

    







