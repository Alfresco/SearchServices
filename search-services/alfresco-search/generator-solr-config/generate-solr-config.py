# Copyright (C) 2020 Alfresco Software Limited.
# This file is part of Alfresco
#
# Alfresco is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Alfresco is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
import itertools

tokenized = "tokenized"
string = "string"
sortable = "sortable"
suggestable = "suggestable"
cross_locale = "cross-locale"
output_file = "generated_copy_fields.xml"


def find_subsets(s, n):
    return [set(i) for x in range(1, n+1) for i in itertools.combinations(s, x) ] 


def get_copy_field_xml(source, destination):
    return '<copyField source="' + source + '" dest="' + destination+ '" />'


def get_dynamic_field_xml(field, field_type):
    postfix = ""
    if field_type in ("text", "content"):
        postfix = '" type="localePrefixedField" />'
    else:
        postfix = '" type="localePrefixedField" multiValued="true" />'

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
    

def generate_fields(field_type, tokenized, string, cross_locale, sortable, suggestable):

    prefix = get_field_prefix(field_type)
    field = prefix + "stored_" + ("t" if tokenized else "_") + ("s" if string else "_")  + ("c" if cross_locale else "_") + ("s" if sortable else "_" ) + ("s" if suggestable else "_") + "@*"
    generated_fields = []
    generated_fields.append(get_dynamic_field_xml(field, field_type))

    if tokenized:
        generated_fields.append(get_copy_field_xml(field, create_tokenized(prefix)))
        if cross_locale:
            generated_fields.append(get_copy_field_xml(field, create_tokenized_cross_locale(prefix)))

    if string:
        generated_fields.append(get_copy_field_xml(field, create_non_tokenized(prefix)))
        if sortable:
            generated_fields.append(get_copy_field_xml(field, create_sortable(prefix)))
        if cross_locale:
            generated_fields.append(get_copy_field_xml(field, create_non_tokenized_cross_locale(prefix)))

    if suggestable:
        generated_fields.append(get_copy_field_xml(field, "suggest"))

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
        s = {tokenized, string, cross_locale, sortable, suggestable} if t == "text" else {tokenized, string, cross_locale, suggestable}
        for s in find_subsets(s, 5):
            generated = generate_fields(t, tokenized in s, string in s, cross_locale in s, sortable in s, suggestable in s)
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
    

    







